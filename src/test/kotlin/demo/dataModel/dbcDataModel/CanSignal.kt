package demo.dataModel.dbcDataModel

import demo.dataModel.udsDataModel.did.CanByteOrder
import io.github.shilic.smartGrid.core.*

/**  用于描述单个信号 */
@GridSheetBind(gridSheetType = GridSheetType.SubSignal)
class CanSignal: IGridRowData {
    // IGridRowData 接口实现
    override val gridKey: String get() = signalName
    override var gridFather: String = ""
    override var gridRowIndex: Int? = null
    /** 信号名称 */
    @GridColumnBind(headerText = "SignalName", pattern = "SignalName", valueType = GridValueType.Text, keyword = true)
    var signalName: String = ""

    /** 分组类型。默认不启用多路复用 */
    //@GridColumnBind()
    var groupType: MatrixGroupType = MatrixGroupType.DefaultGroup

    /** 信号注释 */
    @GridColumnBind(headerText = "SignalDescription", pattern = "SignalDescription", valueType = GridValueType.Text)
    var signalComment: String = ""

    /** 排列格式，Intel 或者 Motorola。排列方式默认为英特尔模式 Intel */
    @GridColumnBind(headerText = "ByteOrder", pattern = "ByteOrder", valueType = GridValueType.Enumeration)
    var byteOrder: CanByteOrder = CanByteOrder.INTEL

    /** 起始位 bit；注意，当数据排列格式为motorola时，存入其中的起始位只能是 MSB 的位置 */
    @GridColumnBind(headerText = "StartBit", pattern = "StartBit", valueType = GridValueType.NumberType)
    var startBit: Int = 0

    /** 信号长度 BitLength(Bit) 会用于最大值最小值的计算 */
    @GridColumnBind(headerText = "BitLength", pattern = "BitLength", valueType = GridValueType.NumberType)
    var bitLength: Int = 0

    /** CAN数据类型,无符号和有符号，默认无符号 */
    @GridColumnBind(headerText = "DataType", pattern = "DataType", valueType = GridValueType.Enumeration)
    var dataType: CanDataType = CanDataType.UNSIGNED

    /** 精度(精度不可以为0，否则无意义) ; 物理值 = 原始值 * factor + offset */
    @GridColumnBind(headerText = "Factor", pattern = "Factor", valueType = GridValueType.NumberType)
    var factor: Double = 0.0

    /** 偏移量 (通常为负数) ； 物理值 = 原始值 * factor + offset */
    @GridColumnBind(headerText = "Offset", pattern = "Offset", valueType = GridValueType.NumberType)
    var offset: Double = 0.0

    /** 物理最小值 */
    @GridColumnBind(headerText = "SignalMinValuePhys", pattern = "SignalMinValuePhys", valueType = GridValueType.NumberType)
    var minValuePhys: Double = 0.0

    /** 物理最大值 */
    @GridColumnBind(headerText = "SignalMaxValuePhys", pattern = "SignalMaxValuePhys", valueType = GridValueType.NumberType)
    var maxValuePhys: Double = 0.0

    /** 物理初始值 */
    @GridColumnBind(headerText = "InitialValuePhys", pattern = "InitialValuePhys", valueType = GridValueType.NumberType)
    var initialValuePhys: Double = 0.0

    //@GridColumnBind(headerText = "", pattern = "", valueType = GridValueType.Enum)
    var minValueHex: UInt = 0u

    //@GridColumnBind(headerText = "", pattern = "", valueType = GridValueType.Enum)
    var maxValueHex: UInt = 0u

    //@GridColumnBind(headerText = "", pattern = "", valueType = GridValueType.Enum)
    var initialValueHex: UInt = 0u

    //@GridColumnBind(headerText = "", pattern = "", valueType = GridValueType.Enum)
    var invalidValueHex: UInt = 0u

    /** 单位 */
    @GridColumnBind(headerText = "Unit", pattern = "Unit", valueType = GridValueType.Text)
    var unit: String = ""

    @GridColumnBind(headerText = "ValueTable", pattern = "ValueTable", valueType = GridValueType.ValueTable)
    var valueTable: MutableMap<Int, String> = mutableMapOf()

    /** 接收节点列表 */
    var sigReceiveNodeSet: MutableSet<String> = mutableSetOf()

    companion object {
        const val VECTOR__XXX = "Vector__XXX"
    }
    override fun toString() = "{信号名称:$signalName, 多路复用:$groupType, 信号注释:$signalComment, 信号排列方式:$byteOrder, 信号起始位:$startBit, 信号长度:$bitLength, 信号数据类型:$dataType, 精度:$factor, 偏移量:$offset, 物理最小值:$minValuePhys, 物理最大值:$maxValuePhys, 单位:$unit }"
}