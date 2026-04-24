package person.shilicheng.core

import org.apache.poi.ss.usermodel.*
import person.shilicheng.exception.ExcelException
import person.shilicheng.utils.*
import kotlin.reflect.KClass
import kotlin.reflect.full.*
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty1

/**
 * 表格解析器，用于解析 Excel 工作簿
 * 使用 Kotlin 的空安全、扩展函数和函数式编程风格重构
 */
class GridParser(private val filePath: String) {
    companion object {
        private const val LOG_TAG = "GridParser"
    }
    /** POI 工作簿 */
    private val workbook: Workbook by lazy { createWorkbook(filePath) }
    /** 使用表名保存对应的表格 */
    private val sheetMap: Map<String, Sheet> by lazy { workbook.getSheetMap() }
    init {
        require(filePath.exists) { "文件不存在，没找到指定 Excel 文件: $filePath" }
    }
    /**
     * 解析指定表格
     * @param objectType 指定表格数据的接收类型，需要在类型定义的字段中标记绑定哪一列
     * @param father 父级元素（可选）
     */
    fun <T : IKey> parse(objectType: KClass<T>, father: IKey? = null): Map<String, T> {
        // 使用类型，解析注解得到表格信息，并尝试从工作表中获取对应表格
        val (sheet, sheetDataType) = checkSheet(objectType)
        // 如果没有该表格，则返回空集合
        if (sheet == null) {
            // println("没有该表格，返回空集合")
            return emptyMap()
        }
        //println("sheet = ${sheet.sheetName}, objectType = ${objectType.simpleName}")
        var rowIndex = 0

        // 传入表格和类型进行解析
        return parseBySheet(sheet, objectType, sheetDataType, rowIndex, father)
    }

    /**
     * 解析一个 sheet，返回包含 sheet 数据的字典
     */
    private fun <T : IKey> parseBySheet(
        sheet: Sheet,
        objectType: KClass<T>,
        sheetDataType: SheetDataType,
        startRowIndex: Int,
        father: IKey? = null
    ): Map<String, T> {
        // 获取表头，输出对应的字段和表头信息
        val (titleRow, columnBindInfos) = getTitle(sheet, objectType)

        // 将对象的字段和表格中的列绑定到一起，记录对应表头的列序号
        getBindColumnIndex(titleRow, columnBindInfos)
        val firstRowIndex = titleRow.rowNum + 1
        val lastRowIndex = sheet.getLastRowIndex()
        // 使用可变 Map 存储结果
        val resultMap = mutableMapOf<String, T>()
        var currentRowIndex = if (sheetDataType == SheetDataType.SubSignal) startRowIndex else firstRowIndex

        // 遍历所有行，添加数据
        loopCells(sheet, sheetDataType, objectType, currentRowIndex, lastRowIndex, columnBindInfos, resultMap, father)

        return resultMap
    }

    /**  遍历所有行，添加数据 */
    private fun <T : IKey> loopCells(
        sheet: Sheet,
        sheetDataType: SheetDataType,
        objectType: KClass<T>,
        startRowIndex: Int,
        lastRowIndex: Int,
        columnBindInfos: List<GridInfo>,
        resultMap: MutableMap<String, T>,
        father: IKey? = null
    ) {
        var rowIndex = startRowIndex
        // 遍历所有行，添加数据
        loopRow@
        while (rowIndex <= lastRowIndex) {
            // 通过反射实例化对象
            val instance = objectType.createInstance()

            // 对于子数据接口，设置父级键
            if (instance is IFatherKey) {
                instance.fatherKey = father?.key ?: ""
            }

            val row = sheet.getRow(rowIndex) ?: continue

            // ----------------------------- 校验第一个单元格 ------------------------
            val firstColumnIndex = columnBindInfos.first().let { bind ->
                requireNotNull(bind.columnIndex) { "第一个绑定字段“${bind.gridBind.headerText}”必须存在列索引" }
            }
            val firstCell = row.getCell(firstColumnIndex)
            /* 如果表格类型 是 SheetDataType.SubSignal，说明正在嵌套解析子信号。
             * 并且第一个单元格(这里的第一个单元格当然就是指的子信号的第一个单元格)为空，则说明子信号解析完成，需要结束全部循环，并返回值。
             * 同时将 rowIndex 记录为 最后一行的下标, rowIndex -= 1 , 回退到上一行, 出栈。*/
            // 处理子信号的特殊退出条件
            if (sheetDataType == SheetDataType.SubSignal && !firstCell.hasContent()) {
                // 回退到上一行，这里会出栈，退出嵌套
                rowIndex -= 1
                break
            }
            // 如果第一个单元格为空且不是子信号，说明这一行没有记录，跳过这一行
            if (sheetDataType != SheetDataType.SubSignal && !firstCell.hasContent()) {
                rowIndex++
                continue
            }
            // 如果第一个元素有删除线，跳过整行
            if (firstCell.isStrikeThrough() && firstCell.hasContent()) {
                rowIndex++
                continue
            }

            // 遍历绑定信息（遍历一行）
            columnBindInfos.forEach { bind ->
                try {
                    when (bind.gridBind.valueType) {
                        /* 获取绑定的列下标。(关键) 如果某一列没有, 说明表格中没有对应的表头，则忽略这个字段，跳过这个单元格。但是不跳过这一行数据。
 * 这里存在的问题是，如果是 BindValueType.OtherPage 类型，因为没有标注正则表达式，所以也不会有列下标，所以无法找到单元格。所以这里单独对这一类型进行提前判断。 */
                        GridValueType.OtherPage -> {
                            val subDataMap = parseOtherPage(bind.property.returnType as KClass<*>, instance)
                            bind.property.setter.call(instance, subDataMap)
                        }
                        else -> {
                            val columnIndex = bind.columnIndex ?: return@forEach
                            val cell = row.getCell(columnIndex)

                            // 针对普通字段，如果单元格为空且不是子信号类型，跳过
                            if (bind.gridBind.valueType != GridValueType.SubSignal && !cell.hasContent()) {
                                return@forEach
                            }

                            val cellValue = cell.stringValue.trim()
                            val parsedValue = parseCellValue(cellValue, bind.gridBind.valueType, bind.property, cell)
                            bind.property.setter?.call(instance, parsedValue)
                        }
                    }
                } catch (e: Exception) {
                    throw ExcelException("解析 Excel 表格出现异常，sheet = ${sheet.sheetName}, 单元格下标 = ${cell.exCell}, exception = ${e.message}")
                }
            }
            // 给对象赋值之后，加入到返回值中
            resultMap[instance.key] = instance
            rowIndex++
        }
    }

    /**
     * 根据单元格值类型进行解析
     */
    private fun parseCellValue(
        cellValue: String,
        valueType: GridValueType,
        property: KMutableProperty1<*, *>,
        cell: Cell
    ): Any = when (valueType) {
        GridValueType.Text -> cellValue
        GridValueType.Number -> parseNumber(cell, property)
        GridValueType.HexNumber -> parseHexNumber(cellValue)
        GridValueType.ValueTable -> parseValueTable(cellValue)
        GridValueType.Enum -> parseEnum(cellValue, property.returnType.classifier as KClass<out Enum<*>>)
        GridValueType.SubSignal -> parseSubSignal(cell.row.sheet, property.returnType, cell.row.rowNum, property.getter.call() as IKey)
        GridValueType.Bool -> parseBool(cellValue)
        GridValueType.StringArray -> parseStringArray(cellValue, property.returnType.classifier as KClass<*>)
        else -> cellValue
    }

    // ======================== 解析器实现 ========================

    /**  解析值描述 */
    private fun parseValueTable(cellValue: String): Map<Int, String> {
        if (cellValue.isBlank()) return emptyMap()

        val pattern = Regex("""(\s*(?<startKey>[\dXxA-Fa-f]+)\s*[-~]?\s*(?<endKey>[\dXxA-Fa-f]+)?\s*[：:=]\s*(?<value>[^\n；;。]+)\s*[\n；;。]?\s*)""")
        val valueTableMap = mutableMapOf<Int, String>()

        pattern.findAll(cellValue).forEach { match ->
            val startKeyString = match.groups["startKey"]?.value ?: return@forEach
            val endKeyString = match.groups["endKey"]?.value
            val value = match.groups["value"]?.value?.trim() ?: return@forEach

            val rectifiedStartKey = rectifyNumber(startKeyString, cellValue)
            val rectifiedEndKey = endKeyString?.let { rectifyNumber(it, cellValue) }

            try {
                val startKey = parseKey(rectifiedStartKey)
                val endKey = rectifiedEndKey?.let { parseKey(it) } ?: startKey

                (startKey..endKey).forEach { key ->
                    valueTableMap[key] = value
                }
            } catch (e: NumberFormatException) {
                throw ExcelException("解析值描述出错，无法解析键值，错误输入是 $startKeyString 或 $endKeyString")
            }
        }

        return valueTableMap
    }

    /**
     * 校验键值是否是数值类型
     */
    private fun rectifyNumber(keyString: String, cellValue: String): String {
        require(isNumber(keyString)) {
            "$cellValue 的值描述有误，其中键 $keyString 的值有误，必须是十六进制数或者十进制数，十六进制数必须加 0x 在前"
        }
        return keyString
    }

    /**
     * 校验是否是数值（十进制或 0x 开头的十六进制数）
     */
    private fun isNumber(strValue: String): Boolean {
        val pattern = Regex("""^((0x|0X)[\dA-Fa-f]+)|(\d+)$""")
        return pattern.matches(strValue)
    }

    /**
     * 将字符串格式的键值转换为整型
     */
    private fun parseKey(keyString: String): Int = when {
        keyString.startsWith("0x", ignoreCase = true) ->
            keyString.substring(2).toInt(16)
        else -> keyString.trim().toInt()
    }

    /**
     * 解析数值类型
     */
    private fun parseNumber(cell: Cell, property:  KMutableProperty1<*,*>): Double {
        val cellText = cell.stringValue.trim()

        val result = when {
            cell.cellType == CellType.NUMERIC -> cell.numericCellValue
            cellText.contains("/") -> cell.parseFraction()
            else -> cellText.toDoubleOrNull() ?: throw ExcelException("无法解析数值: $cellText")
        }

        return checkNumber(result, property, cell)
    }

    private fun checkNumber(number: Double, property:  KMutableProperty1<*,*>, cell: Cell): Double {
        val bind = property.getAnnotation<GridBind>() ?: return number

        return when (bind.headerText) {
            "精度" -> {
                require(number != 0.0) { "精度作为除数，不可以为 0，否则无意义，请重新填写单元格 ${cell.exCell}" }
                number
            }
            else -> number
        }
    }

    /**  解析 16 进制字符串  */
    private fun parseHexNumber(cellValue: String): ULong =
        cellValue.toULongOrNull(16) ?: throw ExcelException("单元格格式错误，无法解析 $cellValue 为 16 进制字符串")

    /**  解析枚举 */
    private inline fun <reified E : Enum<E>> parseEnum(cellValue: String, enumType: KClass<E>): E {
        require(enumType.isSubclassOf(Enum::class)) {
            "类型错误，标注的是枚举类型，实际上的类型却是 ${enumType.simpleName}"
        }

        val enumValues = enumType.java.enumConstants ?: emptyArray()

        // 遍历枚举值的注解进行匹配
        enumValues.forEach { enumValue ->
            val field = enumValue.getDeclaringClass().getField(enumValue.name)
            val bind = field.getAnnotation<GridBind>() ?: return@forEach

            if (bind.pattern.isNotBlank()) {
                val pattern = Regex(bind.pattern)
                if (pattern.containsMatchIn(cellValue)) {
                    return enumValue
                }
            }
        }

        // 返回第一个枚举值作为默认值
        return enumValues.firstOrNull() ?: throw IllegalStateException("枚举类型 ${enumType.simpleName} 没有定义任何值")
    }

    /**
     * 解析子信号
     */
    private fun parseSubSignal(sheet: Sheet, fieldType: Any, currentRowIndex: Int, father: IKey): Map<String, Any> {
        val valueType = getDictionaryValueType(fieldType)
        val sheetDataType = SheetDataType.SubSignal
        val nextRowIndex = currentRowIndex + 1

        // 这里需要根据具体类型进行实例化，简化处理
        return parseBySheet(sheet, valueType, sheetDataType, nextRowIndex, father)
    }

    /**
     * 解析其他页面
     */
    private fun <T : Any>  parseOtherPage(fieldType: KClass<T>, father: IKey): Map<String, Any> {
        val valueType = getDictionaryValueType(fieldType)
        return parse(valueType, father)
    }

    /**
     * 解析布尔值
     */
    private fun parseBool(cellValue: String): Boolean =
        Regex("Y|y|true|True|TRUE").containsMatchIn(cellValue)

    /**
     * 解析字符串数组
     */
    private fun parseStringArray(cellValue: String, dataType: KClass<*>): Any {
        val strings = cellValue.split(',', '，', '、', ';', '；').map { it.trim() }

        return when {
            dataType.isSubclassOf(Array::class) -> strings.toTypedArray()
            dataType.isSubclassOf(List::class) -> strings.toList()
            else -> strings
        }
    }

    // ======================== 辅助方法 ========================

    /**  获取并检查工作表 */
    private fun <T : Any> checkSheet(objectType: KClass<T>): Pair<Sheet?, SheetDataType> {
        val sheetAttr = objectType.findAnnotation<SheetBind>()
            ?: throw ExcelException("无法解析工作表，请先在类型 ${objectType.simpleName} 上使用 SheetBind 注解标注工作表的名称")
        //val sheetDataType = sheetAttr.sheetDataType
        var sheetName = sheetAttr.sheetName

        require(sheetName.isNotBlank()) { "无法解析工作表，请先在类型 ${objectType.simpleName} 的 SheetBind 注解上标注工作表的名称" }

        val patternText = sheetAttr.pattern
        require(patternText.isNotBlank()) { "无法解析工作表，请先在类型 ${objectType.simpleName} 的 SheetBind 注解上标注工作表名称的解析规则 pattern" }

        val pattern = Regex(patternText)
        val matchedSheetName = sheetMap.keys.find { pattern.matches(it) }

        sheetName = matchedSheetName ?: sheetName
        val sheet = workbook.getSheet(sheetName)

        return sheet to sheetAttr.sheetDataType
    }

    /**
     * 解析表头，返回表头所在的行
     */
    private fun <T : Any> getTitle(sheet: Sheet, objectType: KClass<T>): Pair<Row, List<GridInfo> > {
        val columnBindInfos = objectType.getGridInfos()

        val firstGridInfo = columnBindInfos.firstOrNull() ?: throw ExcelException("请确保类型 ${objectType.simpleName} 的字段中标记了 GridBind 注解")

        require(firstGridInfo.gridBind.pattern.isNotBlank()) { "请检查类型 ${objectType.simpleName} 中标记了 GridBind 注解的第一个成员变量上有标注 pattern 属性，否则无法识别表头" }

        val pattern = Regex(firstGridInfo.gridBind.pattern)
        var titleRow: Row? = null

        sheet.forEachCell { cell ->
            val cellValue = cell.stringValue.trim()
            if (pattern.containsMatchIn(cellValue)) {
                titleRow = cell.row
                // 停止遍历
                true
            } else {
                // 继续遍历
                false
            }
        }
        return requireNotNull(titleRow) {
            "表格 ${sheet.sheetName} 中没有找到表头，请检查表格是否有第一个查询项 '${firstGridInfo.gridBind.headerText}'"
        } to columnBindInfos
    }

    /**  获取绑定列索引  */
    private fun getBindColumnIndex(titleRow: Row, columnBindInfos: List<GridInfo>) {
        val lastColumnIndex = titleRow.getLastColumnIndex()
        // 遍历表头
        (0..lastColumnIndex).forEach { columnIndex ->
            val cell = titleRow.getCell(columnIndex) ?: return@forEach
            val value = cell.stringValue.trim()
            columnBindInfos.forEach { bind ->
                if (bind.gridBind.pattern.isNotBlank()) {
                    val pattern = Regex(bind.gridBind.pattern)
                    if (pattern.containsMatchIn(value)) {
                        bind.columnIndex = columnIndex
                    }
                }
            }
        }
    }
}

// ======================== 扩展函数 ========================
/**
 * 获取枚举值的声明字段
 */
private fun <E : Enum<E>> E.getDeclaringClass(): Class<E> =
    this.javaClass.getDeclaringClass() as Class<E>