package demo.dataModel.dbcDataModel

import io.github.shilic.smartGrid.core.GridColumnBind
import io.github.shilic.smartGrid.core.GridSheetBind
import io.github.shilic.smartGrid.core.GridSheetType
import io.github.shilic.smartGrid.core.GridValueType
import io.github.shilic.smartGrid.core.IMutableGridRowData
import io.github.shilic.smartGrid.core.IMutableGridSpecificSheet

@GridSheetBind(sheetName = "DbcList", pattern = "DbcList", gridSheetType = GridSheetType.Dictionary)
class CanDbc : IMutableGridSpecificSheet, IMutableGridRowData {
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

    @GridColumnBind(headerText = "CAN1", valueType = GridValueType.SpecificSheet)
    var canMsgMap: MutableMap<String, CanMessage> = mutableMapOf()

}