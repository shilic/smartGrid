package demo.dataModel.dbcDataModel

import core.*

@GridSheetBind(sheetName = "DbcList", pattern = "DbcList", gridSheetType = GridSheetType.Dictionary)
class CanDbc : IGridSpecificSheet<CanMessage>, IGridRowData {
    // IGridRowData 接口实现
    override val gridKey: String get() = dbcName
    override var gridFather: String = ""
    override var gridRowIndex: Int? = null

    @GridColumnBind(headerText = "DBC英文名", pattern = "DBC英文名", valueType = GridValueType.Text, keyword = true)
    var dbcName: String = ""
    @GridColumnBind(headerText = "DBC描述", pattern = "DBC描述", valueType = GridValueType.Text)
    var dbcComment: String = ""

    @GridColumnBind(headerText = "DBC页面名称", pattern = "DBC页面名称", valueType = GridValueType.Text)
    override var specificSheetName: String = ""
    override val gridDataMap get() = canMsgMap

    @GridColumnBind(headerText = "CAN1", valueType = GridValueType.SpecificSheet)
    var canMsgMap: MutableMap<String, CanMessage> = mutableMapOf()

}