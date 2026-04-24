package person.shilicheng.core

/**
 * 在类上标记该类对应哪一个表格。
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class SheetBind (
    /** 绑定的表格名称 */
    val sheetName : String = "",
    /** 识别表格的正则表达式。程序会到你的excel表格中寻找对应的sheet进行解析 */
    val pattern : String = "",
    /** 表格数据类型, 例如一张表只表示一个数据; 或者一张表表示多个数据, 一行则代表一个 */
    val sheetDataType : SheetDataType = SheetDataType.Dictionary,
)
