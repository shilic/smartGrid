package demo.dataModel.udsDataModel

import core.*
import demo.dataModel.udsDataModel.did.EOLDid

@GridSheetBind(sheetName = "Ecu Info",  pattern = "零部件信息|((Ecu|ecu|ECU)\\\\s*(Info|info|INFO))", gridSheetType = GridSheetType.Single)
class Ecu : IGridRowData {
    // ======================= 1. 接口变量 =======================
    override val gridKey: String get() = ecuName
    override var gridFather: String = ""
    override var gridRowIndex: Int? = null
    // ==================== 2. 零部件基础信息 ====================
    @GridColumnBind("零部件名称", "零部件名称", GridValueType.Text, keyword = true)
    var ecuName: String = ""
    @GridColumnBind("零部件大类", "零部件大类", GridValueType.Text)
    var ecuCategory: String = ""
    @GridColumnBind("厂家名称", "厂家名称", GridValueType.Text)
    var companyName: String = ""
    @GridColumnBind("车型", "车型", GridValueType.Text)
    var model: String = ""
    @GridColumnBind("零部件类型", "零部件类型", GridValueType.Text)
    var ecuType: String = ""

    // ==================== 3. 零部件诊断ID信息 ====================
    @GridColumnBind("物理寻址", "物理寻址", GridValueType.HexNumber)
    var diagRequestId: UInt = 0u
    @GridColumnBind("物理响应", "物理响应", GridValueType.HexNumber)
    var diagResponseId: UInt = 0u
    @GridColumnBind("功能寻址", "功能寻址", GridValueType.HexNumber)
    var functionalRequestId: UInt = 0u
    @GridColumnBind("CAN帧类型", "CAN帧类型", GridValueType.Enum)
    var canFrameType: CanFrameType = CanFrameType.Default

    @GridColumnBind(headerText = "DTCList", valueType = GridValueType.OtherPage)
    var dtcDefineMap : MutableMap<String, Dtc> = mutableMapOf()
    @GridColumnBind(headerText = "EOLConfigDID", valueType = GridValueType.OtherPage)
    var eolDidMap : MutableMap<String, EOLDid> = mutableMapOf()
}