package demo.dataModel.did

import core.GridBind
import core.GridValueType

/**
 * 存储位置 - 使用枚举类
 */
enum class DidStoragePosition(
    @property:GridBind(headerText = "", pattern = "", valueType = GridValueType.Enum)
    val displayName: String
) {
    @GridBind(headerText = "FLASH", pattern = "FLASH")
    FLASH("FLASH"),

    @GridBind(headerText = "EEPROM", pattern = "EEPROM")
    EEPROM("EEPROM"),

    @GridBind(headerText = "ROM", pattern = "ROM")
    ROM("ROM"),

    @GridBind(headerText = "RAM", pattern = "RAM")
    RAM("RAM"),

    @GridBind(headerText = "TBD", pattern = "TBD")
    TBD("TBD");

    companion object {
        fun fromString(name: String): DidStoragePosition? =
            entries.find { it.displayName.equals(name, ignoreCase = true) }
    }
}

/**
 * DID 访问权限 - 使用枚举类
 */
enum class DIDAccessPermission(
    @property:GridBind(headerText = "", pattern = "", valueType = GridValueType.Enum)
    val displayName: String
) {
    @GridBind(
        headerText = "Write",
        pattern = "\\s*(read|Read|READ)\\s*(and|And|AND)\\s*(Write|write|WRITE)\\s*"
    )
    READ_AND_WRITE("Write"),

    @GridBind(headerText = "Readonly", pattern = "(Readonly|readonly|ReadOnly)")
    READONLY("Readonly"),

    @GridBind(headerText = "None", pattern = "None")
    NONE("None");

    companion object {
        fun fromString(name: String): DIDAccessPermission? =
            entries.find { it.displayName.equals(name, ignoreCase = true) }
    }
}

/**
 * DID 编码格式 - 使用枚举类
 * 注意：Kotlin枚举可以带有属性和方法，完全满足需求
 */
enum class DIDCodeFormat(
    val pattern: String,
    val description: String
) {
    @GridBind(
        headerText = "Unsigned",
        pattern = "Hex|hex|HEX|无符号|((Hex|hex|HEX)?\\s*[(（]?\\s*(Unsigned|unsigned|UNSIGNED)\\s*[)）]?)"
    )
    UNSIGNED(
        pattern = "Hex|hex|HEX|无符号|((Hex|hex|HEX)?\\s*[(（]?\\s*(Unsigned|unsigned|UNSIGNED)\\s*[)）]?)",
        description = "无符号类型，例如车辆配置"
    ),

    @GridBind(
        headerText = "Singed",
        pattern = "有符号|(?<!(Un|un|UN))Signed|(?<!(Un|un|UN))signed|(?<!(Un|un|UN))SIGNED"
    )
    SIGNED(
        pattern = "有符号|(?<!(Un|un|UN))Signed|(?<!(Un|un|UN))signed|(?<!(Un|un|UN))SIGNED",
        description = "有符号类型"
    ),

    @GridBind(headerText = "ASCII", pattern = "ASCII|ascii|Ascii")
    ASCII(
        pattern = "ASCII|ascii|Ascii",
        description = "ASCII码格式，用于文本和VIN"
    ),

    @GridBind(headerText = "BCD", pattern = "BCD|bcd|Bcd")
    BCD(
        pattern = "BCD|bcd|Bcd",
        description = "BCD格式，用于日期表示"
    ),

    @GridBind(headerText = "SubData", pattern = "SubData|subData|subdata")
    SUB_DATA(
        pattern = "SubData|subData|subdata",
        description = "表示DID由多个子信号组成"
    ),

    @Deprecated("不再使用二进制格式")
    @GridBind(headerText = "Bin", pattern = "bin|BIN|Bin")
    BIN(
        pattern = "bin|BIN|Bin",
        description = "二进制格式（已弃用）"
    );

    companion object {
        /**
         * 从字符串解析编码格式
         * 使用扩展函数提供更Kotlin风格的API
         */
        fun String.toDidCodeFormat(): DIDCodeFormat? =
            entries.find { format ->
                Regex(format.pattern).matches(this)
            }

        /**
         * 安全地从字符串解析，提供默认值
         */
        fun safeFromString(pattern: String, default: DIDCodeFormat = UNSIGNED): DIDCodeFormat =
            pattern.toDidCodeFormat() ?: default
    }

    /**
     * 检查字符串是否匹配此格式
     */
    fun matches(pattern: String): Boolean =
        Regex(this.pattern).matches(pattern)
}

/**
 * 字节序 - 使用枚举类
 * 注意：Kotlin枚举可以很好地处理这种简单分类
 */
enum class CanByteOrder(
    val displayName: String,
    val isMotorola: Boolean = false
) {
    @GridBind(headerText = "Intel", pattern = "Intel|INTEL|intel")
    INTEL("Intel", false),

    @GridBind(headerText = "Motorola_MSB", pattern = "Motorola_MSB|Motorola MSB|MSB")
    MOTOROLA_MSB("Motorola_MSB", true),

    @GridBind(headerText = "Motorola_LSB", pattern = "Motorola_LSB|Motorola LSB|LSB")
    MOTOROLA_LSB("Motorola_LSB", true);

    companion object {
        /**
         * 从字符串解析字节序
         * 使用命名参数和默认值提供更好的API
         */
        fun fromString(
            name: String,
            ignoreCase: Boolean = true,
            default: CanByteOrder = INTEL
        ): CanByteOrder =
            entries.find { it.displayName.equals(name, ignoreCase) } ?: default

        /**
         * 检查是否是摩托罗拉格式
         */
        fun String.isMotorolaFormat(): Boolean =
            fromString(this).isMotorola
    }

    /**
     * 是否为英特尔格式的扩展属性
     */
    val isIntel: Boolean get() = !isMotorola
}