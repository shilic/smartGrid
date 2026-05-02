package core

import org.apache.poi.ss.usermodel.*
import exception.ExcelException
import utils.*
import kotlin.reflect.KClass
import kotlin.reflect.full.*

/**
 * 表格解析器，用于解析 Excel 工作簿到任意对象
 *
 * @param aWorkbook 传入一个工作表接口 Workbook
 */
class GridParser(private val aWorkbook: Workbook?) : IGridParser {
    companion object {
        private val LOG_TAG = GridParser::class.simpleName
    }
    override val workbook : Workbook by lazy { aWorkbook!! }
    /** 使用表名保存对应的表格 */
    override val sheetMap: Map<String, Sheet> by lazy { workbook.getSheetMap() }
    override val bindCache : Map<KClass<*>, List<GridColumnInfo>> get() = mBindCache
    /** 缓存反射信息。
     *
     * 使用类型作为键， 使用泛型信息类的集合保存这个类的所有
     * */
    private val mBindCache : MutableMap<KClass<*>, List<GridColumnInfo>> = mutableMapOf()
    /** 持有自定义的表格值解析器 */
    private val mCustomGridValueAdapters : MutableMap<String, IGridValueAdapter> = mutableMapOf()

    init {
        // 为什么要这么写？ 因为这是java和kotlin互操作的坑，你传进来非空的java对象，看似非空，实际上也可能为空，故这里强行判断非空。
        requireNotNull(aWorkbook) {"传入的工作簿为空，无法识别表格"}
    }
    override fun registerGridValueAdapter(adapter: IGridValueAdapter) {
        mCustomGridValueAdapters[adapter.adapterName] = adapter
    }

    /**
     * 主函数: 解析指定表格
     * @param objectType 指定表格数据的接收类型，需要在类型定义的字段中标记绑定哪一列
     * @param father 父级元素（可选）
     */
    override fun <T : Any> parse(objectType: KClass<T>, father: IGridRowData?): Map<String, T> {
        require(objectType.isSubclassOf(IGridRowData::class)) { "类型 ${objectType.qualifiedName} 必须实现 ${IGridRowData::class.simpleName} 接口, 才可以被框架解析" }
        // 使用类型，解析注解得到表格信息，并尝试从工作表中获取对应表格
        val (sheet, sheetDataType) = checkSheet(objectType)
        // 如果没有该表格，则返回空集合
        if (sheet == null) { return mutableMapOf() }
        val rowIndex = Ref(0)
        // 传入表格和类型进行解析
        return parseBySheet(sheet, objectType, sheetDataType, rowIndex, father)
    }

    /**
     * 解析一个 sheet，返回包含 sheet 数据的字典
     */
    override  fun <T : Any> parseBySheet(sheet: Sheet, objectType: KClass<T>, gridSheetType: GridSheetType, rowIndex: Ref<Int>, father: IGridRowData?): Map<String, T> {
        // 获取表头，输出对应的字段和表头信息
        val (titleRow, columnBindInfos) = getTitle(sheet, objectType)
        // 将对象的字段和表格中的列绑定到一起，记录对应表头的列序号。这里需要注意，虽然说是同一个反射的结果，但是在不同的表格下，填充的列下标可能是不一样的。
        getBindColumnIndex(titleRow, columnBindInfos)
        // println("columnBindInfos = $columnBindInfos")
        val firstRowIndex = titleRow.rowNum + 1
        // 如果没有抛出异常，则说明表头完整，并且记录了列下标。可以开始正式解析了。
        val lastRowIndex = sheet.getLastRowIndex()
        // 使用可变 Map 存储结果
        val resultMap = mutableMapOf<String, T>()
        // 这里不一样的地方是 rowIndex 由外部传入。如果类型是 SheetDataType.SubSignal ，则说明还是一个页面，则保留行索引。如果不是，则使用外部传入值。
        if (gridSheetType != GridSheetType.SubSignal) {
            rowIndex.value = firstRowIndex
        }
        // 遍历所有行，添加数据
        loopCells(sheet, gridSheetType, objectType, rowIndex, lastRowIndex, columnBindInfos, resultMap, father)
        return resultMap
    }

    /**  遍历所有行，添加数据 */
    private fun <T : Any> loopCells(sheet: Sheet, gridSheetType: GridSheetType, objectType: KClass<T>, rowIndex: Ref<Int>,
        lastRowIndex: Int, columnBindInfos: List<GridColumnInfo>, resultMap: MutableMap<String, T>, father: IGridRowData? = null) {
        // ----------------------------- 外层循环，遍历所有行，添加数据 -------------------------------------
        loopRow@
        while (rowIndex.value <= lastRowIndex) {
            // 通过反射实例化对象
            val instance : T = objectType.createInstance()
            // 对于子数据接口，设置父级键
            if (instance is IGridChild) { instance.gridFather = father?.gridKey ?: "" }
            // 如果行为空就跳过这一行
            val row: Row = sheet.getRow(rowIndex.value) ?: continue@loopRow
            // ----------------------------- 校验第一个单元格 ------------------------
            val keywordBind = columnBindInfos.firstOrNull { bind -> bind.keyword }
                ?: throw IllegalStateException("请检查类型\"${objectType.simpleName}\"的属性上的\"${GridColumnBind::class.simpleName}\"注解, 至少有一个注解了\"${GridColumnBind::keyword.name}\"")
            requireNotNull(keywordBind.columnIndex) { "行数据中注解\"${GridColumnBind::class.simpleName}\"标注了\"${GridColumnBind::keyword.name}\"的字段\"${keywordBind.kMutableProperty}\"必须在表格组件中存在列索引; " +
                    "系统表示未从表格中识别到关键字的列标题: \"${keywordBind.headerText}\" " }
            // 注意，这里获取的单元格是有可能为空的。这是java和kotlin互操作的坑，kotlin中有非空类型，java没有。所以建议一律将java传过来的值视为可空，再去写代码。
            val keywordCell : Cell? = row.getCell(keywordBind.columnIndex!!)
            /* 如果表格类型 是 SheetDataType.SubSignal，说明正在嵌套解析子信号。
             * 并且第一个单元格(这里的第一个单元格当然就是指的子信号的第一个单元格)为空，则说明子信号解析完成，需要结束全部循环，并返回值。
             * 同时将 rowIndex 记录为 最后一行的下标, rowIndex -= 1 , 回退到上一行, 出栈。*/
            // 如果子信号的第一个单元格为空 : 回退到上一行，这里会出栈，退出嵌套 , 同时返回这个下标。
            if (gridSheetType == GridSheetType.SubSignal && !keywordCell.isNotBlank()) {
                rowIndex.value -= 1
                break@loopRow
            }
            // 如果非子信号第一个单元格为空 : 说明这一行没有记录，跳过这一行。如果这一行是子信号，那么也会跳过。但是子信号已经在前边逻辑中解析了。
            if (gridSheetType != GridSheetType.SubSignal && !keywordCell.isNotBlank()) {
                rowIndex.value += 1
                continue@loopRow
            }
            // 如果第一个元素有删除线，跳过整行
            if (keywordCell?.isStrikeThrough() == true && keywordCell.isNotBlank()) {
                rowIndex.value += 1
                continue@loopRow
            }
            // ---------------------------- 内层循环。遍历绑定信息（遍历一行中的所有列） ---------------------------
            loopColumn@
            for (bind in columnBindInfos) {
                //println("bind.valueType = ${bind.valueType}")
                var exCell : String? = null
                val value : Any = try {
                    when (bind.valueType) {
                        // 如果一个字段的注解定义了 识别模式 pattern ，可以识别到单元格，却没有定义解析的规则。就忽略该值，跳过这一个单元格(不跳过一行)。
                        GridValueType.NotDefined -> continue@loopColumn
                        /* 获取绑定的列下标。(关键) 如果某一列没有, 说明表格中没有对应的表头，则忽略这个字段，跳过这个单元格。但是不跳过这一行数据。
                        *  这里存在的问题是，如果是 BindValueType.OtherPage 类型，因为没有标注正则表达式，所以也不会有列下标，所以无法找到单元格。所以这里单独对这一类型进行提前判断。 */
                        GridValueType.OtherSheet, GridValueType.SpecificSheet ->  bind.valueType.parseGridCell(this, null, sheet, bind, rowIndex, instance as IGridRowData)
                        else -> {
                            // 如果没有记录列下标，则跳过这一个单元格(不跳过一行)。
                            val columnIndex = bind.columnIndex ?: continue@loopColumn
                            // 如果单元格为空，则跳过这一个单元格(不跳过一行)。
                            val cell = row.getCell(columnIndex) ?: continue@loopColumn
                            exCell = cell.exCell
                            /* 针对普通字段，如果单元格为空, 或者没有值, 说明表头下对应单元格没有值, 就跳过这个单元格。
                            这里必须使用 && cellValueType != CellValueType_.SubSignal 判断，否则会忽略子信号，无法解析, 因为子信号那一个单元格确实为空。*/
                            if (bind.valueType != GridValueType.SubSignal && !cell.isNotBlank()) { continue@loopColumn }

                            val cellValue = cell.stringValue.trim()
                            when (bind.valueType) {
                                GridValueType.Text, GridValueType.Number, GridValueType.HexNumber, GridValueType.ValueTable,
                                GridValueType.Enum, GridValueType.SubSignal, GridValueType.Bool, GridValueType.Strings,
                                GridValueType.SubStructure
                                    -> bind.valueType.parseGridCell(this, cell, sheet, bind, rowIndex, instance as IGridRowData)
                                GridValueType.Custom -> parseCustomValue(bind.customAdapterName, cell, sheet, bind, rowIndex, instance as IGridRowData)
                                else -> cellValue
                            }
                        }
                    }
                } catch (e: Exception) {
                    throw ExcelException("解析 Excel 表格出现异常，sheet = ${sheet.sheetName}, 单元格下标 = ${exCell}, exception = ${e.message}")
                }
                try {
                    bind.kMutableProperty.setter.call(instance, value)
                }
                catch (e: Exception) {
                    throw ExcelException("[反射设置对象值时出现异常, 接受者类型 : “${instance.javaClass.simpleName}”, 接受者字段类型 : “${bind.kMutableProperty.returnType}”, 接受者字段名称 : “${bind.kMutableProperty.name}”, " +
                            "写入值 : “$value”, 写入值类型 : ${value::class.simpleName}, error : ${e.message} ]")
                }
            }
            // 给对象赋值之后，加入到返回值中
            resultMap[(instance as IGridKey).gridKey] = instance
            rowIndex.value += 1
        }
    }

    // ======================== 解析器实现 ========================
    /** 解析自定义的数据 */
    private fun parseCustomValue(customAdapterName :String, cell: Cell?, sheet: Sheet,  bind : GridColumnInfo, rowIndex: Ref<Int>, father: IGridRowData): Any {
        // 尝试使用名称来获取自定义的解析器
        val custom = mCustomGridValueAdapters[customAdapterName]
            ?: throw IllegalArgumentException("没有找到名为:\"${customAdapterName}\"的自定义表格值适配器，请先使用 ${::registerGridValueAdapter.name} 方法注册")
        return custom.parseGridCell(this, cell, sheet, bind, rowIndex, father)
    }

    // ======================== 辅助方法 ========================

    /**  获取并检查工作表 */
    private fun <T : Any> checkSheet(objectType: KClass<T>): Pair<Sheet?, GridSheetType> {
        val sheetAttr = objectType.findAnnotation<GridSheetBind>()
            ?: throw ExcelException("${LOG_TAG}: 无法解析工作表，请先在类型 ${objectType.simpleName} 上使用 ${GridSheetBind::class.simpleName} 注解标注工作表的名称")
        val sheetName = sheetAttr.sheetName
        require(sheetName.isNotBlank()) {"${LOG_TAG}: 无法解析工作表，请先在类型 ${objectType.simpleName} 的 ${GridSheetBind::class.simpleName} 注解上标注工作表的名称 ${GridSheetBind::sheetName.name} " }
        val patternText = sheetAttr.pattern
        require(patternText.isNotBlank()) {"${LOG_TAG}: 无法解析工作表，请先在类型 ${objectType.simpleName} 的 ${GridSheetBind::class.simpleName} 注解上标注工作表名称的解析规则 ${GridSheetBind::pattern.name} " }

        val pattern = Regex(patternText)
        //使用正则表达式，到表格中去查找对应的 sheet
        val matchedSheetName = sheetMap.keys.find { pattern.matches(it) } ?: sheetName
        val sheet = workbook.getSheet(matchedSheetName)

        return sheet to sheetAttr.gridSheetType
    }

    /**  解析表头。返回表头所在的行，以及所有字段的绑定信息。*/
    private fun <T : Any> getTitle(sheet: Sheet, objectType: KClass<T>): Pair<Row, List<GridColumnInfo> > {
        val columnBindInfos = cacheOrGetBinds(objectType)

        val keywordInfo = columnBindInfos.firstOrNull { bind -> bind.keyword }
            ?: throw ExcelException("请检查类型\"${objectType.simpleName}\"的属性上的\"${GridColumnBind::class.simpleName}\"注解, 至少有一个注解了\"${GridColumnBind::keyword.name}\" ")

        require(keywordInfo.pattern.isNotBlank()) { "请检查类型 ${objectType.simpleName} 中标记了 ${GridColumnBind::class.simpleName} 注解的 ${GridColumnBind::keyword.name} 成员变量上有标注 ${GridColumnBind::pattern.name} 属性，否则无法识别表头" }

        val pattern = Regex(keywordInfo.pattern)
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
            "表格 ${sheet.sheetName} 中没有找到表头，请检查表格是否有第一个查询项 '${keywordInfo.headerText}'"
        } to columnBindInfos
    }

    /** 从缓存中获取反射信息，或者重新使用反射获取 */
    private fun  <T : Any> cacheOrGetBinds(objectType: KClass<T>) : List<GridColumnInfo> {
        return mBindCache.getOrPut(objectType) { objectType.getGridInfos() }
    }

    /**  获取绑定列索引  */
    private fun getBindColumnIndex(titleRow: Row, columnBindInfos: List<GridColumnInfo>) {
        val lastColumnIndex = titleRow.getLastColumnIndex()
        // 遍历表头
        (0..lastColumnIndex).forEach { columnIndex ->
            val cell = titleRow.getCell(columnIndex) ?: return@forEach
            val value = cell.stringValue.trim()
            // 遍历绑定信息
            columnBindInfos.forEach { bind ->
                // 绑定信息记录了列标题的数据, 使用正则表达式查找对应的列，然后记录对应列的列下标。
                if (bind.pattern.isNotBlank()) {
                    // 查找单元格的值是否和正则表达式匹配，如果匹配就记录这个时候的列下标
                    val pattern = Regex(bind.pattern)
                    if (pattern.containsMatchIn(value)) {
                        // 特别注意，因为我们是反射获取的注解信息，这个时候并没有列下标。我们在这里赋值之后，出了这个函数，变量就会无效。
                        bind.columnIndex = columnIndex
                    }
                }
            }
        }
    }
}