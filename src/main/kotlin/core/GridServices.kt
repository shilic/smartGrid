package person.shilicheng.core

/** 一个接口，要求实现类必须给出一个字符串格式的唯一识别键，用于在生成字典的时候用于查询该实现类 */
interface IKey {
    /** 唯一识别键, 只读 */
    val key : String
}

/**  子数据接口，用于指明该子数据的父级元素 */
interface IFatherKey {
    /**  子数据接口，用于指明该子数据的父级元素 */
    var fatherKey : String
}

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
    var aValue : String
}