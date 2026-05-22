package demo.dataModel.udsDataModel.did

import io.github.shilic.smartGrid.core.GridColumnBind

/**
 * 存储位置 - 使用枚举类
 */
enum class DidStoragePosition(val displayName: String) {
    @GridColumnBind(headerText = "FLASH", pattern = "FLASH")
    FLASH("FLASH"),

    @GridColumnBind(headerText = "EEPROM", pattern = "EEPROM")
    EEPROM("EEPROM"),

    @GridColumnBind(headerText = "ROM", pattern = "ROM")
    ROM("ROM"),

    @GridColumnBind(headerText = "RAM", pattern = "RAM")
    RAM("RAM"),

    @GridColumnBind(headerText = "TBD", pattern = "TBD")
    TBD("TBD");
}

/**
 * DID 访问权限 - 使用枚举类
 */
enum class DIDAccessPermission(val displayName: String) {
    @GridColumnBind(headerText = "Write", pattern = "\\s*(read|Read|READ)\\s*(and|And|AND)\\s*(Write|write|WRITE)\\s*")
    READ_AND_WRITE("Write"),

    @GridColumnBind(headerText = "Readonly", pattern = "(Readonly|readonly|ReadOnly)")
    READONLY("Readonly"),

    @GridColumnBind(headerText = "None", pattern = "None")
    NONE("None");
}

/**
 * DID 编码格式 - 使用枚举类
 * 注意：Kotlin枚举可以带有属性和方法，完全满足需求
 */
enum class DIDCodeFormat(
    val description: String
) {
    @GridColumnBind(headerText = "Unsigned", pattern = "Hex|hex|HEX|无符号|((Hex|hex|HEX)?\\s*[(（]?\\s*(Unsigned|unsigned|UNSIGNED)\\s*[)）]?)")
    UNSIGNED(description = "无符号类型，例如车辆配置"),

    @GridColumnBind(headerText = "Singed", pattern = "有符号|(?<!(Un|un|UN))Signed|(?<!(Un|un|UN))signed|(?<!(Un|un|UN))SIGNED")
    SIGNED(description = "有符号类型"),

    @GridColumnBind(headerText = "ASCII", pattern = "ASCII|ascii|Ascii")
    ASCII(description = "ASCII码格式，用于文本和VIN"),

    @GridColumnBind(headerText = "BCD", pattern = "BCD|bcd|Bcd")
    BCD(description = "BCD格式，用于日期表示"),

    @GridColumnBind(headerText = "SubData", pattern = "SubData|subData|subdata")
    SUB_DATA(description = "表示DID由多个子信号组成"),

    @Deprecated("不再使用二进制格式")
    @GridColumnBind(headerText = "Bin", pattern = "bin|BIN|Bin")
    BIN(description = "二进制格式（已弃用）");
}

/**
 * 字节序 - 使用枚举类
 * 注意：Kotlin枚举可以很好地处理这种简单分类
 */
enum class CanByteOrder(val displayName: String) {
    @GridColumnBind(headerText = "Intel", pattern = "Intel|INTEL|intel")
    INTEL("Intel"),

    @GridColumnBind(headerText = "Motorola_MSB", pattern = "Motorola_MSB|Motorola MSB|MSB")
    MOTOROLA_MSB("Motorola_MSB"),

    @GridColumnBind(headerText = "Motorola_LSB", pattern = "Motorola_LSB|Motorola LSB|LSB")
    MOTOROLA_LSB("Motorola_LSB");
    /**  是否为英特尔格式的扩展属性 */
    val isIntel: Boolean get() = (this == INTEL)
    val isMotorola: Boolean get() = (this != INTEL)
}