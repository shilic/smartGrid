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
    @GridColumnBind(headerText = "零部件名称", pattern = "零部件名称", valueType =  GridValueType.Text, keyword = true)
    var ecuName: String = ""
    @GridColumnBind(headerText = "零部件大类", pattern = "零部件大类", valueType = GridValueType.Text)
    var ecuCategory: String = ""
    @GridColumnBind(headerText = "厂家名称", pattern = "厂家名称", valueType = GridValueType.Text)
    var companyName: String = ""
    @GridColumnBind(headerText = "车型", pattern = "车型",valueType =  GridValueType.Text)
    var model: String = ""
    @GridColumnBind(headerText = "零部件类型", pattern = "零部件类型",valueType =  GridValueType.Text)
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

//    // 使用嵌套的解析逻辑, 解析一个简单的嵌套对象也是可以的。
//    @GridColumnBind(headerText = "物理寻址", pattern = "物理寻址", valueType = GridValueType.SubStructure)
//    var diagInfo : EcuDiagInfo = EcuDiagInfo()

    @GridColumnBind(headerText = "DTCList", valueType = GridValueType.OtherSheet)
    var dtcDefineMap : MutableMap<String, Dtc> = mutableMapOf()
    @GridColumnBind(headerText = "EOLConfigDID", valueType = GridValueType.OtherSheet)
    var eolDidMap : MutableMap<String, EOLDid> = mutableMapOf()
}

@GridSheetBind(gridSheetType = GridSheetType.SubStructure)
@Deprecated("弃用")
class EcuDiagInfo : IGridRowData {
    @OptIn(ExperimentalStdlibApi::class)
    override val gridKey: String get() = diagRequestId.toHexString(HexFormat.UpperCase)
    override var gridFather: String = ""
    override var gridRowIndex: Int? = null
    @GridColumnBind(headerText = "物理寻址", pattern = "物理寻址", valueType = GridValueType.HexNumber, keyword = true)
    var diagRequestId: UInt = 0u
    @GridColumnBind(headerText = "物理响应", pattern = "物理响应", valueType =  GridValueType.HexNumber)
    var diagResponseId: UInt = 0u
    @GridColumnBind(headerText = "功能寻址", pattern = "功能寻址", valueType =  GridValueType.HexNumber)
    var functionalRequestId: UInt = 0u
    @GridColumnBind(headerText = "CAN帧类型", pattern = "CAN帧类型", valueType = GridValueType.Enum)
    var canFrameType: CanFrameType = CanFrameType.Default
}