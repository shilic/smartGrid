package core

import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import kotlin.reflect.KClass

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
interface IGridRowIndex {
    /** 表格数据行下标接口，用于记录对象的行下标 */
    var gridRowIndex : Int?
}
/** 行数据接口。作用于类上，用于规范描述一行数据。
 *
 * 包括: 行数据的关键字、行数据的父级元素、行下标。 */
interface IGridRowData : IGridKey, IGridChild, IGridRowIndex

/** table接口。用于指示该数据是一个 完整表格 + 额外数据。
 *
 * 区别于直接在字段上使用 MutableMap 标记数据表。使用该接口之后，还可以在数据表之上，额外添加一些字段给这个对象。
 *
 * 再给 tableMap 字段的内部字段标记 GridBind 注解， GridBind 注解的属性设置为 GridValueType.SpecificTable， 表示从特定表格进行解析。
 * 表格名称从本接口的 specificTable 属性获取，而不是从 泛型 T 上边的表格注解 SheetBind 获取。
 *
 * 例如: 适用于有多个相同结构的表(例如多个DID，或者多个DBC)，但是你又不想使用字段来单独表示这些表，你想直接使用一个 Map 来表示这些相似的表，然后通过这些额外的数据来区分这些表，而不是字段名称和字段注解。
 *
 * @param T 泛型T，需要实现 IGridData 接口。表示该 IGridSpecificSheet 接口负责管理何种类型的数据。
 * */
interface IGridSpecificSheet<T : IGridRowData> {
    /* 实现类自己需要定义自己的额外字段，以对应表格中的列标题。 */
    /** 在表格组件中的表格名称，框架会根据该变量的名称，到表格组件中去寻找对应的工作表。 */
    val specificSheetName: String
    /** 持有的表格数据 */
    val gridDataMap: MutableMap<String, T>
}

interface IGridParser {
    val workbook : Workbook
    val sheetMap: Map<String, Sheet>
    /** 缓存反射信息。
     *
     * 使用类型作为键， 使用泛型信息类的集合保存这个类的所有
     * */
    val bindCache : Map<KClass<*>, List<GridColumnInfo>>
    /** 注册表格值解析器 */
    fun registerGridValueAdapter(adapter: IGridValueAdapter)
    fun <T : Any> parse(objectType: KClass<T>, father: IGridRowData? = null): Map<String, T>
    fun <T : Any> parseBySheet(sheet: Sheet, objectType: KClass<T>, gridSheetType: GridSheetType, rowIndex: Ref<Int>, father: IGridRowData? = null): Map<String, T>
}


/** 子数据拥有者, 使用行序号保存子数据对象 */
interface SubDataOwner {
    /**  使用行序号保存子数据对象 */
    var subObjectMap : MutableMap<Int, Any>
}
/**  指示填入表格中的数据源，哪一个是值描述， 以及值描述选项 */
interface IValueTable {
    /** 指示填入表格中的数据源，哪一个是值描述 */
    var valueTable : MutableMap<Int, String>
    /** 值描述中具体取了哪一个值  */
    var aValue : String
}