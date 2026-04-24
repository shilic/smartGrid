package person.shilicheng.core

/**
 * 表格数据类型, 例如一张表只表示一个数据; 或者一张表表示多个数据, 一行则代表一个。又或者表示子数据。
 */
enum class SheetDataType {
    /**
     * 一张表只表示一个数据
     */
    Single,
    /**
     * 一张表表示多个数据, 一行则代表一个
     */
    Dictionary,
    /**
     * 解析子信号
     */
    SubSignal,
}