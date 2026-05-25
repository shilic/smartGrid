package io.github.shilic.smartGrid.attributes


enum class AttributeValueType(
    /** 值类型对应的KClass */
    val valueClass: Class<*>
) {
    /** 整数 值的完整元信息 */
    IntegerType(Int::class.java),
    /** 浮点数的完整元信息 */
    DoubleType(Double::class.java),
    /** 字符串的完整元信息 */
    StringType(String::class.java),
    /** 枚举的完整元信息 */
    Enumeration(String::class.java),
    /** 十六进制的完整元信息 */
    HexValue(Int::class.java);
}