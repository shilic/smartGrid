package core

/** 一个接口，要求实现类必须给出一个字符串格式的唯一识别键，在生成字典的时候作为该实现类的字典的键 */
interface IGridKey {
    /** 一个接口，要求实现类必须给出一个字符串格式的唯一识别键，在生成字典的时候作为该实现类的字典的键 */
    val gridKey : String
}

/**  表格数据的子数据接口，用于指明该子数据的父级元素 */
interface IGridChild {
    /**  表格数据的子数据接口，用于指明该子数据的父级元素 */
    var gridFather : String
}
/** 表格数据行下标接口，用于记录对象的行下标 */
interface IGridRow {
    /** 表格数据行下标接口，用于记录对象的行下标 */
    var gridRowIndex : Int?
}
/** 表格数据接口。用于规范描述一行数据。
 *
 * 包括: 行数据的关键字、行数据的父级元素、行下标。 */
interface IGridData : IGridKey, IGridChild, IGridRow

/** 子数据拥有者, 使用行序号保存子数据对象 */
interface SubDataOwner {
    /**  使用行序号保存子数据对象 */
    var subObjectMap : MutableMap<Int, Any>
}

/**  指示填入表格中的数据源，哪一个是值描述， 以及值描述选项 */
interface IValueTable {
    /** 指示填入表格中的数据源，哪一个是值描述 */
    val valueTable : MutableMap<Int, String>
    /** 值描述中具体取了哪一个值  */
    var currentValue : String
}