package demo.dataModel.udsDataModel

import io.github.shilic.smartGrid.core.GridColumnBind

enum class CanFrameType(val value: Byte) {
    @GridColumnBind("标准帧", "Standard|standard|stand|Stand|标准帧")
    Standard(0),
    @GridColumnBind("扩展帧", "Extended|extended|Extend|extend|扩展帧")
    Extended(1),
    Default(0xFF.toByte());
}