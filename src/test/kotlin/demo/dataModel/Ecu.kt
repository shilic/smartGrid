package demo.dataModel

import core.*

@SheetBind(sheetName = "Ecu Info",  pattern = "零部件信息|((Ecu|ecu|ECU)\\\\s*(Info|info|INFO))", sheetDataType = SheetDataType.Single)
class Ecu : IGridData {
    // ======================= 1. 接口变量 =======================
    override val gridKey: String get() = ecuName
    override var gridFather: String = ""
    override var gridRowIndex: Int? = null
    // ==================== 2. 零部件基础信息 ====================
    @GridBind("零部件名称", "零部件名称", GridValueType.Text)
    var ecuName: String = ""
    @GridBind("零部件大类", "零部件大类", GridValueType.Text)
    var ecuCategory: String = ""
    @GridBind("厂家名称", "厂家名称", GridValueType.Text)
    var companyName: String = ""
    @GridBind("车型", "车型", GridValueType.Text)
    var model: String = ""
    @GridBind("零部件类型", "零部件类型", GridValueType.Text)
    var ecuType: String = ""

    // ==================== 3. 零部件诊断ID信息 ====================
    @GridBind("物理寻址", "物理寻址", GridValueType.HexNumber)
    var diagRequestId: UInt = 0u
    @GridBind("物理响应", "物理响应", GridValueType.HexNumber)
    var diagResponseId: UInt = 0u
    @GridBind("功能寻址", "功能寻址", GridValueType.HexNumber)
    var functionalRequestId: UInt = 0u
    @GridBind("CAN帧类型", "CAN帧类型", GridValueType.Enum)
    var canFrameType: CanFrameType = CanFrameType.Default

    @GridBind(headerText = "DTCList", valueType = GridValueType.OtherPage)
    var dtcDefineMap : MutableMap<String, Dtc> = mutableMapOf()
}