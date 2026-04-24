package person.shilicheng.core

/**  诊断调查表中，单元格的值类型，决定了该单元格会按照何种格式解析 */
enum class GridValueType {
    /** 未定义 */
    NotDefined,
    /**  默认，按照字符串格式(文本格式)解析 */
    Default,
    /**  文本格式，按照普通字符串格式解析 */
    Text,
    /**  按照16进制字符串解析成整形数 */
    HexNumber,
    /** 普通数值类型(十进制) */
    Number,
    /**  按照值描述来解析，字段类型是 Dictionary (int ,string) 的字段就是值描述，按需标注该注解。
     * 通常用于描述这个值的解析规则。 */
    ValueTable,
    /**  按照枚举来解析。拿到枚举字段后，会获取该枚举类型的所有枚举项，并获取上边的注解，
     * 再拿到正则表达式，进行单元格识别。 */
    Enum,
    /**  解析子信号，对象的字段出现该注解时，表明需要在当前表格嵌套解析子信号。 */
    SubSignal,
    /**  表示该值直接继承自父级元素的同类值。  */
    @Deprecated("弃用，未使用")
    Inherited,
    /**  解析子页面，表示这个数据是在另外一个表格定义的，需要到该字段的定义中寻找信息来解析。  */
    OtherPage,
    /**  委托  */
    Function,
    /**  算法, 包括"安全算法"和"bootloader流程"的算法 */
    MethodType,
    /**  布尔类型 */
    Bool,
    /**  字符串数组，或字符串集合  */
    StringArray
}