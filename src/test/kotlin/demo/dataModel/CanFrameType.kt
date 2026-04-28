package demo.dataModel

import core.GridBind

enum class CanFrameType(val value: Byte) {
    @GridBind("标准帧", "Standard|standard|stand|Stand|标准帧")
    Standard(0),
    @GridBind("扩展帧", "Extended|extended|Extend|extend|扩展帧")
    Extended(1),
    Default(0xFF.toByte());
}