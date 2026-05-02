package demo.dataModel.dbcDataModel

import core.*

@GridSheetBind(sheetName = "CanProtocol_Info", pattern = "CanProtocol_Info", gridSheetType = GridSheetType.Single)
class CanProtocol : IGridRowData {
    // IGridRowData 接口实现
    override val gridKey: String get() = protocolName
    override var gridFather: String = ""
    override var gridRowIndex: Int? = null

    @GridColumnBind(headerText = "协议名称", pattern = "协议名称", valueType = GridValueType.Text, keyword = true)
    var protocolName: String = ""
    @GridColumnBind(headerText = "协议描述", pattern = "协议描述", valueType = GridValueType.Text)
    var protocolComment: String = ""
    @GridColumnBind(headerText = "DbcList", valueType = GridValueType.OtherSheet)
    var dbcMap: MutableMap<String, CanDbc> = mutableMapOf()
}