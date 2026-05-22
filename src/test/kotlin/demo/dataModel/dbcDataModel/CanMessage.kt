package demo.dataModel.dbcDataModel

import io.github.shilic.smartGrid.core.GridColumnBind
import io.github.shilic.smartGrid.core.GridSheetBind
import io.github.shilic.smartGrid.core.GridSheetType
import io.github.shilic.smartGrid.core.GridValueType
import io.github.shilic.smartGrid.core.IGridRowData

/**
 * 用于描述消息 Message
 */
@GridSheetBind(gridSheetType = GridSheetType.Dictionary)
class CanMessage : IGridRowData {
    // IGridRowData 接口实现
    override val gridKey: String get() = "0x${msgId.toString(16).uppercase().padStart(8, '0')}"
    override var gridFather: String = ""
    override var gridRowIndex: Int? = null
    /** 报文名称, 非空 */
    @GridColumnBind(headerText = "报文名称", pattern = "报文名称", valueType = GridValueType.Text, uiIgnore = true, keyword = true)
    var msgName: String = ""

    /** 报文标识符 */
    @GridColumnBind(headerText = "报文标识符", pattern = "报文标识符", valueType = GridValueType.HexNumber)
    var msgId: UInt = 0u

//    /** 报文标识符的DBC编码 */
//    var msgIdCode: Long = 0L
//    /** 信号类型默认为 扩展帧 Extended */
//    var canFrameType: CanFrameType = CanFrameType.Extended
//    /** 报文发送类型 默认为周期型 */
//    var msgSendType: MsgSendType = MsgSendType.Cycle

    /** 报文周期时间 毫秒 */
    @GridColumnBind(headerText = "报文周期时间", pattern = "报文周期时间", valueType = GridValueType.Number)
    var msgCycleTime: Int = 0

    /** 报文长度 单位: byte */
    @GridColumnBind(headerText = "报文长度", pattern = "报文长度", valueType = GridValueType.Number)
    var msgLength: Byte = 0

    /** 报文注释 */
    @GridColumnBind(headerText = "Remark", pattern = "Remark", valueType = GridValueType.Text)
    var msgComment: String = ""

//    /** 发送节点, 当前报文的节点名称, 节点名称默认为 Vector__XXX */
//    var nodeName: String = "Vector__XXX"
//    /** 发送节点列表 */
//    var msgSendNodeList: MutableSet<String> = mutableSetOf()

    /** 信号列表 ; 键指信号的名称, 值指的是信号 */
    @GridColumnBind(headerText = "SignalName", pattern = "SignalName", valueType = GridValueType.SubSignal)
    var signalMap: MutableMap<String, CanSignal> = mutableMapOf()

    override fun toString() = "消息名称:$msgName, 报文标识符:${msgId.toString(16)}, 报文周期(单位:毫秒):$msgCycleTime,报文长度(单位byte):$msgLength,|报文注释:$msgComment,消息所含信号数量:${signalMap.size}"
    companion object {
        /**
         * 转换 idCode至 id。公式如下 : msgIDCode = msg_ID + 0x8000_0000L ;  //仅针对扩展帧
         * @param msgIDCode 当扩展帧时，DBC文件中的id值
         * @return 返回真实id值
         */
        fun transIdCodeToID(msgIDCode: Long): Int {
            return (msgIDCode - 0x8000_0000L).toInt()
        }

        /**
         * 转换 idCode至 id。公式如下 : msgIDCode = msg_ID + 0x8000_0000L ;  仅针对扩展帧
         * @param msgId 真实id值
         * @return 返回 当扩展帧时，DBC文件中的id值
         */
        fun transIdToIdCode(msgId: Long): Long {
            return msgId + 0x8000_0000L
        }
    }
}