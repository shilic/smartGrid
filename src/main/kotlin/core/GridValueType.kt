package core

import exception.ExcelException
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Sheet
import utils.getDictionaryKeyValueTypes
import utils.parseFraction
import utils.safeAsEnumClass
import utils.stringValue
import java.math.BigDecimal
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**  诊断调查表中，单元格的值类型，决定了该单元格会按照何种格式解析 */
enum class GridValueType(
    /** 枚举的实际值 */
    val index: Int,
    /** 枚举描述 */
    val description: String,
) : IGridValueAdapter {
    /** 未定义，表示初始值，需要向上层报错, 或者忽略该值。 */
    NotDefined(-1, "未定义") {
        override fun parseGridCell(gridParser:IGridReader, cell: Cell?, sheet: Sheet, bind : GridColumnInfo, rowIndex: Ref<Int>, father: IGridRowData): Any {
            throw NotImplementedError("${NotDefined.description}: 类型:\"${father::class.simpleName}\"上的属性:\"${bind.kMutableProperty}\"中的注解\"${GridColumnBind::class.simpleName}\"" +
                    "未定义${GridValueType::class.simpleName}属性，无法确定以什么方式解析该属性。请在该注解上定义${GridValueType::class.simpleName}")
        }
    },
    /**  文本格式。按照普通字符串格式解析; 通常是 String 类型 */
    Text(0, "文本格式") {
        override fun parseGridCell(gridParser:IGridReader, cell: Cell?, sheet: Sheet, bind : GridColumnInfo, rowIndex: Ref<Int>, father: IGridRowData): Any {
            return cell!!.stringValue.trim()
        }
    },
    /** 普通数值类型(十进制)。最终会被识别成 Double 或者 Float */
    Number(1, "普通数值类型(十进制)") {
        override fun parseGridCell(gridParser:IGridReader, cell: Cell?, sheet: Sheet, bind : GridColumnInfo, rowIndex: Ref<Int>, father: IGridRowData): Any {
            val kClass = bind.kMutableProperty.returnType.classifier as KClass<*>
            val cellValue = cell!!.stringValue.trim()
            return when {
                // 分数特殊，需要按照分数格式解析
                cellValue.contains("/") -> cell.parseFraction()
                else -> {
                    val value : Double = cellValue.toDoubleOrNull() ?: throw ExcelException("无法解析Double类型数值: $cellValue")
                    when (kClass) {
                        Byte::class -> value.toInt().toByte()
                        Short::class -> value.toInt().toShort()
                        Int::class -> value.toInt()
                        Long::class -> value.toLong()

                        UByte::class -> value.toUInt().toUByte()
                        UShort::class -> value.toUInt().toUShort()
                        UInt::class -> value.toUInt()
                        ULong::class -> value.toULong()

                        Float::class -> value.toFloat()
                        Double::class -> value

                        BigDecimal::class -> cellValue.toBigDecimalOrNull() ?: throw ExcelException("单元格格式错误，无法解析 $cellValue 为 BigDecimal")
                        else -> value
                    }
                }
            }
        }
    },
    /**  按照16进制字符串解析成整形数。 通常是  UInt 或者 UShort 类型 */
    HexNumber(2, "16进制字符串") {
        override fun parseGridCell(gridParser:IGridReader, cell: Cell?, sheet: Sheet, bind : GridColumnInfo, rowIndex: Ref<Int>, father: IGridRowData): Any {
            val kClass = bind.kMutableProperty.returnType.classifier as KClass<*>
            val cellValue = cell!!.stringValue.trim()
            val value : ULong = cellValue.trim().lowercase().replace("0x", "").toULongOrNull(16) ?: throw ExcelException("单元格格式错误，无法解析 $cellValue 为 16 进制字符串")
            return when (kClass) {
                Byte::class -> value.toByte()
                Short::class -> value.toShort()
                Int::class -> value.toInt()
                Long::class -> value.toLong()

                UByte::class -> value.toUByte()
                UShort::class -> value.toUShort()
                UInt::class -> value.toUInt()
                ULong::class -> value

                BigDecimal::class -> cellValue.toBigDecimalOrNull() ?: throw ExcelException("单元格格式错误，无法解析 $cellValue 为 BigDecimal")
                else -> value
            }
        }
    },
    /** 值描述。字段类型是 Dictionary (int ,string) 的字段就是值描述，按需标注该注解。
     *  通常用于描述这个值的解析规则。 */
    ValueTable(3, "值描述") {
        override fun parseGridCell(gridParser:IGridReader, cell: Cell?, sheet: Sheet, bind : GridColumnInfo, rowIndex: Ref<Int>, father: IGridRowData): Any {
            val cellValue = cell!!.stringValue.trim()
            return parseValueTable(cellValue)
        }
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
    },
    /**  按照枚举来解析。拿到枚举字段后，会获取该枚举类型的所有枚举项，并获取上边的注解， 再拿到正则表达式，进行单元格识别。 */
    Enum(4, "枚举") {
        override fun parseGridCell(gridParser:IGridReader, cell: Cell?, sheet: Sheet, bind : GridColumnInfo, rowIndex: Ref<Int>, father: IGridRowData): Any {
            val cellValue = cell!!.stringValue.trim()
            val kClass = bind.kMutableProperty.returnType.classifier as KClass<*>
            require(kClass.java.isEnum) {"类型错误，${GridColumnBind::class.simpleName}注解标注的是枚举类型，实际上的类型却是 ${kClass.simpleName}" }
            val enumClass = kClass.safeAsEnumClass() ?: throw IllegalArgumentException("类型错误，标注的是枚举类型，实际上的类型却是 ${kClass.simpleName}")
            // 获取枚举的所有值
            val enumConstants = enumClass.java.enumConstants
            require(enumConstants.isNotEmpty()) {"枚举类型 ${enumClass.simpleName} 没有定义任何值"}

            // 遍历所有枚举值
            for (enum in enumConstants) {
                // 获取注解
                val field = enumClass.java.getField(enum.name)
                val gridColumnBind = field.getAnnotation(GridColumnBind::class.java)
                if (gridColumnBind == null || gridColumnBind.pattern.isBlank()) { continue }
                // 进行正则匹配
                if (Regex(gridColumnBind.pattern).matches(cellValue)) { return enum }
            }
            throw IllegalStateException( "枚举类型 ${enumClass.simpleName} 没有找到与值 '$cellValue' 匹配的项" )
        }
    },
    /**  子信号。对象的字段出现该注解时，表明需要在当前表格嵌套解析子信号。 */
    SubSignal(5, "子信号") {
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
        override fun parseGridCell(gridParser:IGridReader, cell: Cell?, sheet: Sheet, bind : GridColumnInfo, rowIndex: Ref<Int>, father: IGridRowData): Any {
            // 获取字典类型的值的类型 valueType ，作为下一个函数的输入
            val (_, valueClass)  = bind.kMutableProperty.getDictionaryKeyValueTypes()
            require(valueClass.isSubclassOf(IGridRowData::class)) { "类:\"${father::class.simpleName}\"的属性:\"${bind.kMutableProperty.name}\"中, 字典嵌套的值的类型也必须实现\"${IGridRowData::class.simpleName}\"接口 " }
            // 强制指定是 SheetDataType.SubSignal 类型。
            val gridSheetType = GridSheetType.SubSignal
            // 这里需要手动将下标移动到下一行。特别注意，在退出条件里，需要将下标 -1 以 回到上一行。
            rowIndex.value += 1

            // 这里需要根据具体类型进行实例化，简化处理
            return gridParser.readBySheet(sheet, valueClass, gridSheetType, rowIndex, father)
        }
    },
    /**  子页面。表示这个数据是在另外一个表格定义的，需要到该字段的定义中寻找信息来解析。  */
    OtherSheet(6, "子页面") {
        override fun parseGridCell(gridParser:IGridReader, cell: Cell?, sheet: Sheet, bind : GridColumnInfo, rowIndex: Ref<Int>, father: IGridRowData): Any {
            // 传入字段的类型，理论上是 如 public Dictionary<string, EOLDid> ; 获取字典类型的值的类型 valueType ，作为下一个函数的输入
            val  (_, valueClass)  = bind.kMutableProperty.getDictionaryKeyValueTypes()
            require(valueClass.isSubclassOf(IGridRowData::class)) { "类:\"${father::class.simpleName}\"的属性:\"${bind.kMutableProperty.name}\"中, 字典嵌套的值的类型也必须实现\"${IGridRowData::class.simpleName}\"接口 " }
            // 再将值的类型传入，进行嵌套调用
            return gridParser.read(valueClass, father)
        }
    },
    /**  布尔类型; 通常值的类型为 true 和 false 。 */
    Bool(7, "布尔") {
        override fun parseGridCell(gridParser:IGridReader, cell: Cell?, sheet: Sheet, bind : GridColumnInfo, rowIndex: Ref<Int>, father: IGridRowData): Any {
            val cellValue = cell!!.stringValue.trim()
            return Regex("Y|y|true|True|TRUE").matches(cellValue.trim())
        }
    },
    /**  文本集合。 字符串数组、字符串List集合 或者 字符串Set集合。 使用逗号分割每一个数据元素。  */
    Strings(8, "文本集合") {
        override fun parseGridCell(gridParser:IGridReader, cell: Cell?, sheet: Sheet, bind : GridColumnInfo, rowIndex: Ref<Int>, father: IGridRowData): Any {
            val cellValue = cell!!.stringValue.trim()
            val kClass = bind.kMutableProperty.returnType.classifier as KClass<*>
            val strings : List<String> = cellValue.split(',', '，', '、', ';', '；').map { it.trim() }

            return when {
                kClass.isSubclassOf(Array::class) -> strings.toTypedArray()
                kClass.isSubclassOf(List::class) -> strings.toList()
                kClass.isSubclassOf(Set::class) -> strings.toSet()
                else -> strings
            }
        }
    },
    /** 子结构类型。表示嵌套表示一个简单对象(或结构)。数据的来源还是原来的表格，只不过数据不是依附在主对象上，而是在嵌套结构上。 */
    @Deprecated("不实用，决定弃用")
    SubStructure(9, "子结构") {
        /* 子结构类型和子数据类型类似，只不过不需要移动行下标 */
        override fun parseGridCell(gridParser: IGridReader, cell: Cell?, sheet: Sheet, bind: GridColumnInfo, rowIndex: Ref<Int>, father: IGridRowData): Any {
            val valueClass = bind.kMutableProperty.returnType.classifier as KClass<*>
            require(valueClass.isSubclassOf(IGridRowData::class)) { "类:\"${father::class.simpleName}\"的属性:\"${bind.kMutableProperty.name}\"中, 子结构类型也必须实现\"${IGridRowData::class.simpleName}\"接口 " }
            val gridSheetType = GridSheetType.SubStructure
            val subStructures = gridParser.readBySheet(sheet, valueClass, gridSheetType, rowIndex, father)
            return subStructures.values.first()
        }
    },
    /** 特定表格。需要和 IGridSpecificSheet 接口一起使用，用于从特定的表格中获取表格数据。从 IGridSpecificSheet 接口中的 specificSheetName 变量获取特定表格的名称后，再进行下一步解析 */
    SpecificSheet(10, "特定表格") {
        override fun parseGridCell(gridParser: IGridReader, cell: Cell?, sheet: Sheet, bind: GridColumnInfo, rowIndex: Ref<Int>, father: IGridRowData): Any {
            // 校验
            require(father is IGridSpecificSheet<*>) { "类:\"${father::class.simpleName}\"必须实现\"${IGridSpecificSheet::class.simpleName}\"接口来指定数据表类型和数据表名称, 否则无法使用枚举项${SpecificSheet}来解析特定表格 " }
            val (_, valueClass)  = bind.kMutableProperty.getDictionaryKeyValueTypes()
            require(valueClass.isSubclassOf(IGridRowData::class)) { "类:\"${father::class.simpleName}\"的属性:\"${bind.kMutableProperty.name}\"中, 字典嵌套的值的类型也必须实现\"${IGridRowData::class.simpleName}\"接口 " }

            // 校验完成后，需要将父级元素转换为 IGridTable ，并且提取出来 specificTableName;
            val specificTableName = (father as IGridSpecificSheet<*>).specificSheetName
            require(specificTableName.isNotBlank()) { "想要使用${SpecificSheet}模式来解析特定表格，\"${IGridSpecificSheet::class.simpleName}\"接口中的${IGridSpecificSheet<*>::specificSheetName.name}变量值必须非空(非null和非空白字符)" }

            // 拿到特定表格名称后,尝试获取表格
            val newSheet: Sheet = gridParser.sheetMap[specificTableName] ?: throw ExcelException("没有在传入的表格组件中找到名为:\"${specificTableName}\"的子表格(Sheet/Page/Tab)")
            // 由于是新的sheet，所以这里我们使用全新的参数
            val gridSheetType = GridSheetType.Dictionary
            val newRowIndex = Ref(0)
            return gridParser.readBySheet(newSheet, valueClass, gridSheetType, newRowIndex, father)
        }
    },
    /** 使用自定义的解析适配器。使用该数据类型时，使用字符串标注适配器的名称，并且需要额外添加适配器到表格解析器当中。 */
    Custom(11, "自定义适配器") {
        override fun parseGridCell(gridParser:IGridReader, cell: Cell?, sheet: Sheet, bind : GridColumnInfo, rowIndex: Ref<Int>, father: IGridRowData): Any {
            throw NotImplementedError("请不要单独使用${Custom::class.simpleName}的解析方法, 而是使用框架进行注册后，由框架调用自定义解析规则。")
        }
    };
    override val adapterName: String get() = description
}