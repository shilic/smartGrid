package demo.dataModel.udsDataModel

import core.*

/**
 * 诊断故障码 (DTC) 数据类
 * 使用密封接口实现更类型安全的多态行为
 */
@GridSheetBind(sheetName = "DTC List", pattern = "\\s*(DTC|dtc|Dtc)\\s*(List|list|LIST)\\s*", gridSheetType = GridSheetType.Dictionary)
class Dtc  : IGridRowData {
    // ======================= 1. 接口变量 =======================
    override val gridKey: String get() = dtcHexNumber.toHexString()
    override var gridFather: String = ""
    override var gridRowIndex: Int? = null

    // ====================== 2. 故障核心信息 ====================
    /** 主键：DTC 的 16 进制值 */
    @GridColumnBind(headerText = "DtcHexNumber", pattern = "DTC值|DtcHexNumber|((DTC|dtc|Dtc)\\s*(Byte|byte|BYTE))", valueType = GridValueType.HexNumber, keyword = true)
    var dtcHexNumber: UInt = 0u
    /** DTC 英文名称 */
    @GridColumnBind(headerText = "DTC英文名称", pattern = "DTC英文名称", valueType = GridValueType.Text)
    var dtcName: String = ""
    /** DTC 含义描述 */
    @GridColumnBind(headerText = "DTC含义", pattern = "DTC含义", valueType = GridValueType.Text)
    var dtcMeaning: String = ""

    // ===================== 3. 额外故障信息 =====================
    // 故障检测前置条件
    @GridColumnBind(headerText = "故障检测前置条件", pattern = "故障检测前置条件", valueType = GridValueType.Text)
    var preconditions: String = ""
    // 故障触发条件
    @GridColumnBind(headerText = "故障触发条件", pattern = "故障触发条件", valueType = GridValueType.Text)
    var triggerConditions: String = ""
    // 故障恢复条件
    @GridColumnBind(headerText = "故障恢复条件", pattern = "故障恢复条件", valueType = GridValueType.Text)
    var recoveryConditions: String = ""
    // 故障严重等级
    @GridColumnBind(headerText = "故障严重等级", pattern = "故障严重等级", valueType = GridValueType.Text)
    var faultLevel: String = ""
    // 系统表现
    @GridColumnBind(headerText = "系统表现", pattern = "系统表现", valueType = GridValueType.Text)
    var systemAction: String = ""
    // 可能故障原因
    @GridColumnBind(headerText = "可能故障原因", pattern = "可能故障原因", valueType = GridValueType.Text)
    var possibleFaultCauses: String = ""
    // 维修建议
    @GridColumnBind(headerText = "维修建议", pattern = "维修建议", valueType = GridValueType.Text)
    var correctiveAction: String = ""
    // 特殊指示
    @GridColumnBind(headerText = "特殊指示", pattern = "特殊指示", valueType = GridValueType.Text)
    var specialIndication: String = ""
    // 备注
    @GridColumnBind(headerText = "备注", pattern = "备注", valueType = GridValueType.Text)
    var remarks: String = ""

    // ==================== 故障码状态 =================
    /** 故障状态掩码，用于记录故障状态。
     * 在接收故障码时，如果本地已有DTC则赋值，否则新建DTC 。*/
    @Transient
    @GridColumnBind(headerText = "故障掩码", valueType = GridValueType.HexNumber)
    var dtcStatusMaskValue: Byte = 0

    // --------------------- 计算属性 ---------------------
    /**  DTC 显示码（如 C056789），从 16 进制值解析而来;
     * 计算属性，每次访问时动态计算 */
    @delegate:Transient
    @GridColumnBind(headerText = "DTC显示码", valueType = GridValueType.Text, uiIgnore = true)
    val dtcDisplay: String by lazy { parseDtcHexToDisplay(dtcHexNumber) }
    /**
     * DTC 故障系统区域/故障属性;
     * 从 16 进制值的高两位解析
     */
    @delegate:Transient
    @GridColumnBind(headerText = "故障属性", valueType = GridValueType.Enum)
    val dtcSystemType: DtcSystemType by lazy { parseDtcHexToSystemType(dtcHexNumber) }

    // --------------------- 公共方法 -----------------------

    /**
     * 友好的字符串表示
     */
    override fun toString(): String =
        "DTCHex: ${dtcHexNumber.toHexString()}, " +
                "故障系统区域: ${dtcSystemType.description}, " +
                "故障掩码: ${dtcStatusMaskValue.toHexString()}, " +
                "DTC描述: $dtcMeaning"

    // ---------- 伴生对象（静态方法） ----------

    companion object {
        /**
         * 将 16 进制 DTC 值解析为显示码
         * 如 0x00456789 → "C056789"
         */
        fun parseDtcHexToDisplay(hexValue: UInt): String {
            val systemType = parseDtcHexToSystemType(hexValue)
            val otherValue = hexValue and 0x3F_FFFFu  // 0b_0011_1111__1111_1111__1111_1111

            val systemChar = when (systemType) {
                DtcSystemType.POWER_TRAIN -> "P"
                DtcSystemType.CHASSIS -> "C"
                DtcSystemType.BODY -> "B"
                DtcSystemType.NETWORK -> "U"
            }

            return systemChar + otherValue.toString(16).uppercase().padStart(6, '0')
        }
        /**
         * 解析 DTC 16 进制值，返回系统类型
         */
        fun parseDtcHexToSystemType(hexValue: UInt): DtcSystemType {
            //  如 DTC 的 Hex 值是 0x456789; 取最高字节（24-31位）的最高两位; 取出 最高位的 0x45，对应的下标是2;
            val highestByte = (hexValue shr 16).toByte()
            //  获取 16-23 位; // 取出 0x45 最高 2 bit，(最高字节转换成二进制是 0100_0101b)
            val systemTypeBits = (highestByte.toInt() shr 6) and 0b11

            return DtcSystemType.entries
                .firstOrNull { it.value == systemTypeBits }
                ?: throw IllegalArgumentException("无效的系统类型位: $systemTypeBits")
        }
    }

    // ---------- 扩展函数 ----------

    /**
     * 将 Byte 转换为十六进制字符串
     */
    private fun Byte.toHexString(): String =
        "0x${toString(16).uppercase().padStart(2, '0')}"

    /**
     * 将 UInt 转换为十六进制字符串
     */
    private fun UInt.toHexString(): String =
        "0x${toString(16).uppercase().padStart(8, '0')}"
}

/**
 * DTC 系统类型枚举
 * 使用注解为枚举值添加描述信息
 */
enum class DtcSystemType (
    val value: Int,
    val description: String
) {
    POWER_TRAIN(0b00, "动力系统"),
    CHASSIS(0b01, "底盘系统"),
    BODY(0b10, "车身系统"),
    NETWORK(0b11, "网络系统");

    companion object {
        /**  通过值查找枚举，更安全的替代方案 */
        fun fromValue(value: Int): DtcSystemType? =
            entries.firstOrNull { it.value == value }
    }
}