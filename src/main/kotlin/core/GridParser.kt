package core

import org.apache.poi.ss.usermodel.*
import exception.ExcelException
import utils.*
import java.math.BigDecimal
import kotlin.reflect.KClass
import kotlin.reflect.full.*
import kotlin.reflect.KType

/**
 * 表格解析器，用于解析 Excel 工作簿到任意对象
 */
class GridParser(private val filePath: String) {
    companion object {
        private val LOG_TAG = GridParser::class.simpleName
    }
    /** POI 工作簿 */
    private val workbook: Workbook by lazy { createWorkbook(filePath) }
    /** 使用表名保存对应的表格 */
    private val sheetMap: Map<String, Sheet> by lazy { workbook.getSheetMap() }
    init {
        require(filePath.isNotBlank()) { "文件路径为空，无法识别文件。" }
        require(filePath.exists) { "文件不存在，没找到指定文件: $filePath" }
        val fileExtension = filePath.fileExtension.lowercase()
        require(SUPPORTED_EXCEL_EXTENSIONS.contains(fileExtension)){"${LOG_TAG}: 不支持的文件格式: $fileExtension, 必须是\"xls\", \"xlsx\"文件"}
    }
    /**
     * 主函数: 解析指定表格
     * @param objectType 指定表格数据的接收类型，需要在类型定义的字段中标记绑定哪一列
     * @param father 父级元素（可选）
     */
    fun <T : Any> parse(objectType: KClass<T>, father: IGridData? = null): Map<String, T> {
        require(objectType.isSubclassOf(IGridData::class)) { "类型 ${objectType.qualifiedName} 必须实现 ${IGridData::class.simpleName} 接口" }
        // 使用类型，解析注解得到表格信息，并尝试从工作表中获取对应表格
        val (sheet, sheetDataType) = checkSheet(objectType)
        // 如果没有该表格，则返回空集合
        if (sheet == null) { return createTypedMap(String::class, objectType) }
        val rowIndex = RefValue(0)
        // 传入表格和类型进行解析
        return parseBySheet(sheet, objectType, sheetDataType, rowIndex, father)
    }

    /**
     * 解析一个 sheet，返回包含 sheet 数据的字典
     */
    private fun <T : Any> parseBySheet(
        sheet: Sheet,
        objectType: KClass<T>,
        sheetDataType: SheetDataType,
        rowIndex: RefValue<Int>,
        father: IGridData? = null
    ): Map<String, T> {
        // 获取表头，输出对应的字段和表头信息
        val (titleRow, columnBindInfos) = getTitle(sheet, objectType)

        // 将对象的字段和表格中的列绑定到一起，记录对应表头的列序号
        getBindColumnIndex(titleRow, columnBindInfos)
        val firstRowIndex = titleRow.rowNum + 1
        // 如果没有抛出异常，则说明表头完整，并且记录了列下标。可以开始正式解析了。
        val lastRowIndex = sheet.getLastRowIndex()
        // 使用可变 Map 存储结果
        val resultMap : MutableMap<String, T> = createTypedMap(String::class, objectType)
        // 这里不一样的地方是 rowIndex 由外部传入。如果类型是 SheetDataType.SubSignal ，则说明还是一个页面，则保留行索引。如果不是，则使用外部传入值。
        if (sheetDataType != SheetDataType.SubSignal) {
            rowIndex.value = firstRowIndex
        }
        // 遍历所有行，添加数据
        loopCells(sheet, sheetDataType, objectType, rowIndex, lastRowIndex, columnBindInfos, resultMap, father)
        return resultMap
    }

    /**  遍历所有行，添加数据 */
    private fun <T : Any> loopCells(
        sheet: Sheet,
        sheetDataType: SheetDataType,
        objectType: KClass<T>,
        rowIndex: RefValue<Int>,
        lastRowIndex: Int,
        columnBindInfos: List<GridInfo>,
        resultMap: MutableMap<String, T>,
        father: IGridData? = null
    ) {
        // 遍历所有行，添加数据
        loopRow@
        while (rowIndex.value <= lastRowIndex) {
            // 通过反射实例化对象
            val instance : T = objectType.createInstance()

            // 对于子数据接口，设置父级键
            if (instance is IGridChild) {
                instance.gridFather = father?.gridKey ?: ""
            }
            else {
                println("${objectType.simpleName} 未实现 IChild")
            }

            val row = sheet.getRow(rowIndex.value) ?: continue

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
                // 回退到上一行，这里会出栈，退出嵌套 , 同时返回这个下标。
                rowIndex.value -= 1
                break
            }
            // 如果第一个单元格为空且不是子信号，说明这一行没有记录，跳过这一行。如果这一行是子信号，那么也会跳过。但是子信号已经在嵌套逻辑中解析了。
            if (sheetDataType != SheetDataType.SubSignal && !firstCell.hasContent()) {
                rowIndex.value++
                continue
            }
            // 如果第一个元素有删除线，跳过整行
            if (firstCell.isStrikeThrough() && firstCell.hasContent()) {
                rowIndex.value++
                continue
            }

            // 遍历绑定信息（遍历一行）
            columnBindInfos.forEach { bind ->
                var exCell : String? = null
                try {
                    when (bind.gridBind.valueType) {
                        GridValueType.NotDefined -> throw  IllegalArgumentException("")
                        /* 获取绑定的列下标。(关键) 如果某一列没有, 说明表格中没有对应的表头，则忽略这个字段，跳过这个单元格。但是不跳过这一行数据。
                        *  这里存在的问题是，如果是 BindValueType.OtherPage 类型，因为没有标注正则表达式，所以也不会有列下标，所以无法找到单元格。所以这里单独对这一类型进行提前判断。 */
                        GridValueType.OtherPage -> {
                            //println("解析其他页面 : ${bind.gridBind.headerText}，当前元素Key: “${(instance as IKey).gridKey}” ")
                            val subDataMap = parseOtherPage(bind.kMutableProperty.returnType, instance as IGridData)
                            bind.kMutableProperty.setter.call(instance, subDataMap)
                        }
                        else -> {
                            //println("解析字段 : ${bind.gridBind.headerText}，当前元素Key: “${(instance as IKey).gridKey}” ")
                            val columnIndex = bind.columnIndex ?: return@forEach
                            val cell = row.getCell(columnIndex)
                            exCell = cell.exCell
                            /* 针对普通字段，如果单元格为空, 或者没有值, 说明表头下对应单元格没有值, 就跳过这个单元格。
                            这里必须使用 && cellValueType != CellValueType_.SubSignal 判断，否则会忽略子信号，无法解析, 因为子信号那一个单元格确实为空。*/
                            if (bind.gridBind.valueType != GridValueType.SubSignal && !cell.hasContent()) {
                                return@forEach
                            }

                            val cellValue = cell.stringValue.trim()
                            val parsedValue = when (bind.gridBind.valueType) {
                                GridValueType.Text, GridValueType.Default -> cellValue
                                GridValueType.Number -> parseNumber(cell, bind.kMutableProperty.returnType.classifier as KClass<*>)
                                GridValueType.HexNumber -> parseHexNumber(cellValue, bind.kMutableProperty.returnType.classifier as KClass<*>)
                                GridValueType.ValueTable -> parseValueTable(cellValue)
                                GridValueType.Enum -> parseEnum(cellValue, bind.kMutableProperty.returnType.classifier as KClass<*>)
                                GridValueType.SubSignal -> parseSubSignal(cell.row.sheet, bind.kMutableProperty.returnType, rowIndex, instance as IGridData)
                                GridValueType.Bool -> parseBool(cellValue)
                                GridValueType.Strings -> parseStringArray(cellValue, bind.kMutableProperty.returnType.classifier as KClass<*>)
                                GridValueType.CustomAdapter -> parseCustomValue(cell, cell.row.sheet, bind.kMutableProperty.returnType, rowIndex)
                                else -> cellValue
                            }
                            bind.kMutableProperty.setter.call(instance, parsedValue)
                        }
                    }
                } catch (e: Exception) {
                    throw ExcelException("解析 Excel 表格出现异常，sheet = ${sheet.sheetName}, 单元格下标 = ${exCell}, exception = ${e.message}")
                }
            }
            // 给对象赋值之后，加入到返回值中
            resultMap[(instance as IGridKey).gridKey] = instance
            rowIndex.value++
        }
    }

    // ======================== 解析器实现 ========================

    /** 1. 解析值描述 */
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
                throw ExcelException("解析值描述出错，无法解析键值，错误输入是 $startKeyString 或 $endKeyString, error : ${e.message}")
            }
        }
        return valueTableMap
    }
    /**  校验键值是否是数值类型 */
    private fun rectifyNumber(keyString: String, cellValue: String): String {
        require(isNumber(keyString.trim())) {
            "$cellValue 的值描述有误，其中键 $keyString 的值有误，必须是十六进制数或者十进制数，十六进制数必须加 0x 在前"
        }
        return keyString.trim()
    }
    /**  校验是否是数值（十进制或 0x 开头的十六进制数） */
    private fun isNumber(strValue: String): Boolean {
        val pattern = Regex("""^((0x|0X)[\dA-Fa-f]+)|(\d+)$""")
        return pattern.matches(strValue.trim())
    }
    /** 将字符串格式的键值转换为整型 */
    private fun parseKey(keyString: String): Int = when {
        keyString.startsWith("0x", ignoreCase = true) ->
            keyString.substring(2).toInt(16)
        else -> keyString.trim().toInt()
    }

    /** 2. 解析数值类型 */
    private fun parseNumber(cell: Cell, kClass : KClass<*>): Any {
        val cellValue = cell.stringValue.trim()
        return when {
            cell.cellType == CellType.NUMERIC -> cell.numericCellValue
            cellValue.contains("/") -> cell.parseFraction()
            else -> {
                val value = cellValue.toDoubleOrNull() ?: throw ExcelException("无法解析Double类型数值: $cellValue")
                when (kClass) {
                    Double::class -> value
                    Float::class -> value.toFloat()
                    BigDecimal::class -> cellValue.toBigDecimalOrNull() ?: throw ExcelException("单元格格式错误，无法解析 $cellValue 为 BigDecimal")
                    else -> value
                }
            }
        }
    }

    /** 3. 解析 16 进制字符串  */
    private fun parseHexNumber(cellValue: String, kClass : KClass<*>): Any {
        val value = cellValue.trim().lowercase().replace("0x", "").toULongOrNull(16) ?: throw ExcelException("单元格格式错误，无法解析 $cellValue 为 16 进制字符串")
        return when (kClass) {
            Int::class -> value.toInt()
            Long::class -> value.toLong()
            Short::class -> value.toShort()
            Byte::class -> value.toByte()
            UInt::class -> value.toUInt()
            ULong::class -> value
            UShort::class -> value.toUShort()
            UByte::class -> value.toUByte()
            BigDecimal::class -> cellValue.toBigDecimalOrNull() ?: throw ExcelException("单元格格式错误，无法解析 $cellValue 为 BigDecimal")
            else -> value
        }
    }

    /** 4. 解析枚举 - 不使用具体化类型参数 */
    private fun parseEnum(cellValue: String, clazz: KClass<*>): Enum<*> {
        require(clazz.java.isEnum) {"类型错误，标注的是枚举类型，实际上的类型却是 ${clazz.simpleName}" }
        val enumClass = clazz.safeAsEnumClass() ?: throw IllegalArgumentException("类型错误，标注的是枚举类型，实际上的类型却是 ${clazz.simpleName}")
        // 获取枚举的所有值
        val enumConstants = enumClass.java.enumConstants
        require(enumConstants.isNotEmpty()) {"枚举类型 ${enumClass.simpleName} 没有定义任何值"}

        // 遍历所有枚举值
        for (enum in enumConstants) {
            // 获取注解
            val field = enumClass.java.getField(enum.name)
            val gridBind = field.getAnnotation(GridBind::class.java)
            if (gridBind == null || gridBind.pattern.isBlank()) { continue }
            // 进行正则匹配
            if (Regex(gridBind.pattern).matches(cellValue)) { return enum }
        }
        throw IllegalStateException( "枚举类型 ${enumClass.simpleName} 没有找到与 '$cellValue' 匹配的项" )
    }
    // 注意，这里无法像之前一样，返回一个Dictionary<string, V> 泛型类型，因为我们不清楚具体的泛型，只能传入一个类型 Type 。
    // 所以只有使用自定义的类型 Map 进行解析，  然后在最后强制转换为 Dictionary<string, V> ，并通过 object 返回。
    /* 解析子信号步骤。
     * 1. 当解析到字段的标记类型是 CellValueType_.SubSignal，触发相关逻辑。传入该字段的类型进行解析，例如 Did 存在子信号  public Dictionary<string, DidSignal> signalMap;
     * 2. 解析子信号的值的类型。再查找类型上的 SheetBindAttribute 注解查找新的表头。
     * 3. 和 parse 方法唯一的区别就是，起始的行下标是由外部传入。
     * 4. 并且退出条件不一样 : 当解析到第一个单元格为删除线时，进行跳过整行；检查到第一个单元格为空时，结束整个循环，将结果返回到外部。注意：这里实际上会忽略掉父级元素的单元格, 相当于排除了干扰项。
     * 5. 同时，将解析到的值返回给外部。并记录最后一行下标。
     * 6. 注意，有无子数据都会嵌套进入子数据识别的逻辑。只是说，如果没有子数据，则会返回没有元素的空集合。
     */
    /**  5. 解析子信号 */
    private fun parseSubSignal(sheet: Sheet, fieldType: KType, rowIndex: RefValue<Int>, father: IGridData): Map<String, *> {
        // 获取字典类型的值的类型 valueType ，作为下一个函数的输入
        val valueType = fieldType.getDictionaryKeyValueTypes().second
        // 强制指定是 SheetDataType.SubSignal 类型。
        val sheetDataType = SheetDataType.SubSignal
        // 这里需要手动将下标移动到下一行。特别注意，在退出条件里，需要将下标 -1 以 回到上一行。
        rowIndex.value++

        // 这里需要根据具体类型进行实例化，简化处理
        return parseBySheet(sheet, valueType, sheetDataType, rowIndex, father)
    }

    /**  6. 解析其他页面 */
    private fun parseOtherPage(fieldType: KType, father: IGridData): Map<String, *> {
        //println("解析其他页面，传入父级元素: “${father.gridKey}” ")
        // 传入字段的类型，理论上是 如 public Dictionary<string, EOLDid> ; 获取字典类型的值的类型 valueType ，作为下一个函数的输入
        val valueType = fieldType.getDictionaryKeyValueTypes().second
        // 再将值的类型传入，进行嵌套调用
        return parse(valueType, father)
    }

    /** 7. 解析布尔值 */
    private fun parseBool(cellValue: String): Boolean = Regex("Y|y|true|True|TRUE").matches(cellValue.trim())

    /** 8. 解析字符串数组 */
    private fun parseStringArray(cellValue: String, dataType: KClass<*>): Any {
        val strings : List<String> = cellValue.split(',', '，', '、', ';', '；').map { it.trim() }

        return when {
            dataType.isSubclassOf(Array::class) -> strings.toTypedArray()
            dataType.isSubclassOf(List::class) -> strings.toList()
            dataType.isSubclassOf(Set::class) -> strings.toSet()
            else -> strings
        }
    }

    /** 9. 解析自定义的数据 */
    private fun parseCustomValue(cell : Cell, sheet: Sheet, fieldType: KType, rowIndex: RefValue<Int>): Any? {
        return  null
    }

    // ======================== 辅助方法 ========================

    /**  获取并检查工作表 */
    private fun <T : Any> checkSheet(objectType: KClass<T>): Pair<Sheet?, SheetDataType> {
        val sheetAttr = objectType.findAnnotation<SheetBind>()
            ?: throw ExcelException("${LOG_TAG}: 无法解析工作表，请先在类型 ${objectType.simpleName} 上使用 SheetBind 注解标注工作表的名称")
        val sheetName = sheetAttr.sheetName
        require(sheetName.isNotBlank()) {"${LOG_TAG}: 无法解析工作表，请先在类型 ${objectType.simpleName} 的 SheetBind 注解上标注工作表的名称 sheetName " }
        val patternText = sheetAttr.pattern
        require(patternText.isNotBlank()) {"${LOG_TAG}: 无法解析工作表，请先在类型 ${objectType.simpleName} 的 SheetBind 注解上标注工作表名称的解析规则 pattern " }

        val pattern = Regex(patternText)
        //使用正则表达式，到表格中去查找对应的 sheet
        val matchedSheetName = sheetMap.keys.find { pattern.matches(it) } ?: sheetName
        val sheet = workbook.getSheet(matchedSheetName)

        return sheet to sheetAttr.sheetDataType
    }

    /**  解析表头。返回表头所在的行，以及所有字段的绑定信息。*/
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
            // 遍历绑定信息
            columnBindInfos.forEach { bind ->
                // 绑定信息记录了列标题的数据, 使用正则表达式查找对应的列，然后记录对应列的列下标。
                if (bind.gridBind.pattern.isNotBlank()) {
                    // 查找单元格的值是否和正则表达式匹配，如果匹配就记录这个时候的列下标
                    val pattern = Regex(bind.gridBind.pattern)
                    if (pattern.containsMatchIn(value)) {
                        // 特别注意，因为我们是反射获取的注解信息，这个时候并没有列下标。我们在这里赋值之后，出了这个函数，变量就会无效。
                        bind.columnIndex = columnIndex
                    }
                }
            }
        }
    }
}