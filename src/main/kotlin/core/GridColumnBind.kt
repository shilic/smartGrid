package core

import java.lang.ref.WeakReference
import kotlin.reflect.KMutableProperty1

/** 注解： 用于按字段(属性)绑定对象到表格。
 *
 * headerText : 列标题; pattern : 列标题的正则表达式; valueType : 绑定值的类型; uiIgnore : UI层是否忽略; columnWidth : 列宽; customAdapter : 自定义的适配器名称;
 * */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class GridColumnBind (
    /** headerText : 列标题(为空则直接忽略，不添加到表格，或者直接不添加本注解)。 在表格类UI组件(如Grid或者DataGridView时)上显示该字段时，使用该属性作为列标题。 */
    val headerText : String = "",
    /** pattern : 列标题的正则表达式。 用于识别表头位置，以及嵌套识别枚举类型的枚举项。如果该正则表达式没有值就表示不从表格中识别它。*/
    val pattern : String = "",
    /** valueType : 绑定值的类型。 字符串类型、Double数值类型 还是 16 进制数值类型等 */
    val valueType : GridValueType = GridValueType.NotDefined,
    /** uiIgnore : UI层是否忽略。 在表格类UI组件(如Grid或者DataGridView时)上显示该字段时，UI层是否忽略，默认 false 不忽略 */
    val uiIgnore : Boolean = false,
    /** columnWidth : 列宽。 在表格类UI组件(如Grid或者DataGridView时)上显示该字段时的列宽*/
    val columnWidth : Int = 200,
    /** customAdapter : 自定义的适配器名称。 当 绑定值的类型 valueType 为  GridValueType.CustomAdapter时，使用该字段;
     * 用于获取自定义的适配器，再由框架调用自定义的识别规则。 */
    val customAdapterName : String = "",
    /** keyword : 关键字 */
    val keyword : Boolean = false,
)
/**  扩展函数，让GridBind可以直接绑定到 KMutableProperty1<,> 属性。
 *
 * 确保多次从注解绑定到字段时，都会获取到同一个 GridColumnInfo 类。
 * */
fun GridColumnBind.bindTo(kMutableProperty: KMutableProperty1<*,*>): GridColumnInfo {
    return GridColumnInfo.getOrCreate(this, kMutableProperty)
}
/**  Grid 注解的扩展信息。
 *
 * =============================
 *
 * 包括原来的注解信息 = headerText : 列标题; pattern : 列标题的正则表达式; valueType : 绑定值的类型; uiIgnore : UI层是否忽略; columnWidth : 列宽; customAdapter : 自定义的适配器名称。
 *
 * ==============================
 *
 * 以及额外信息 = columnIndex : 记录列下标; kMutableProperty : 注解的持有者(某个可变属性); cacheKey : 该注解的键 ;
 *
 * ==============================
 *
 * 因为 kotlin 的注解的属性必须是不可变的，而我恰好又需要到注解上添加可变的属性，所以我需要扩展注解。
 * */
class GridColumnInfo private constructor (
    /** columnIndex : 记录列下标。 可能为空，表示没有列下标 */
    var columnIndex: Int? = null,
    /** gridBind : 持有一个 Grid 注解  */
    val gridColumnBind: GridColumnBind,
    /** kMutableProperty : 注解的持有者(某个可变属性)。 该注解和哪一个属性绑定 */
    val kMutableProperty: KMutableProperty1<*,*>,
    /** 如果该字段是一个子数据的键值对，还需要记录子数据的列序号和扩展信息键值对 TODO */
    @Deprecated("暂时没有使用场景，之后再来使用")
    val subGridInfo: MutableList<GridColumnInfo> = mutableListOf(),
    /** cacheKey : 该注解的键 */
    val cacheKey : String,
) : IGridKey
{
    /** headerText : 列标题(为空则直接忽略，不添加到表格，或者直接不添加本注解)。 在表格类UI组件(如Grid或者DataGridView时)上显示该字段时，使用该属性作为列标题。 */
    val headerText : String get() = gridColumnBind.headerText
    /** pattern : 列标题的正则表达式。 用于识别表头位置，以及嵌套识别枚举类型的枚举项。如果该正则表达式没有值就表示不从表格中识别它。*/
    val pattern : String get() = gridColumnBind.pattern
    /** valueType : 绑定值的类型。 字符串类型、Double数值类型 还是 16 进制数值类型等 */
    val valueType : GridValueType get() = gridColumnBind.valueType
    /** uiIgnore : UI层是否忽略。 在表格类UI组件(如Grid或者DataGridView时)上显示该字段时，UI层是否忽略，默认 false 不忽略 */
    val uiIgnore : Boolean get() = gridColumnBind.uiIgnore
    /** columnWidth : 列宽。 在表格类UI组件(如Grid或者DataGridView时)上显示该字段时的列宽*/
    val columnWidth : Int get() = gridColumnBind.columnWidth
    /** customAdapterName : 自定义的适配器名称。 当 绑定值的类型 valueType 为  GridValueType.Custom时，使用该字段;
     * 用于获取自定义的适配器，再由框架调用自定义的识别规则。 */
    val customAdapterName : String get() = gridColumnBind.customAdapterName
    /** keyword : 关键字。用于在从表格组件识别值的时候，判断哪一个单元格是这一行的关键字，关键字的单元格需要不为空。 */
    val keyword : Boolean get() = gridColumnBind.keyword

    override val gridKey  get() = cacheKey
    override fun toString(): String {
        return "{ headerText: ${headerText}, pattern: ${pattern}, valueType: ${valueType}, uiIgnore: ${uiIgnore}, columnWidth: ${columnWidth}, customAdapter: ${customAdapterName}, keyword: ${keyword}, columnIndex: $columnIndex, kMutableProperty: ($kMutableProperty), cacheKey: $cacheKey, }\n"
    }
    /** 检查绑定信息是否有效，有效才会加入到UI界面中;
     * 不为空，有标题(用于添加表格列标题)，有数据类型(会根据数据类型添加对应格式的列，文本、数值或者下拉框), 并且没有被忽略(uiIgnore = false)。
     */
    val validity : Boolean get() = headerText.isNotBlank() && valueType != GridValueType.NotDefined && !gridColumnBind.uiIgnore
    companion object {
        /**  使用弱引用缓存所有的 GridBind 注解 ，避免内存泄漏 */
        private val cache = mutableMapOf<String, WeakReference<GridColumnInfo>>()
        /**  通过工厂创建实例, 获取或创建GridInfo 。 确保多次从注解绑定到字段时，都会获取到同一个 GridColumnInfo 类。 */
        fun getOrCreate (gridColumnBind: GridColumnBind, kMutableProperty: KMutableProperty1<*,*>): GridColumnInfo {
            val cacheKey = createCacheKey(gridColumnBind, kMutableProperty)
            val cached : GridColumnInfo? = cache[cacheKey]?.get()
            // 如果缓存命中了，就直接返回一个 GridColumnInfo 对象；如果没有命中缓存，就重新获取; 并添加到缓存中保存，确保反复获取时可以获取同一个值。
            return cached ?: GridColumnInfo( gridColumnBind = gridColumnBind, kMutableProperty = kMutableProperty, cacheKey = cacheKey)
                .also { newInstance -> cache[cacheKey] = WeakReference(newInstance)}
        }
        /**  创建唯一识别的缓存键。确保多次从注解绑定到字段时，都会获取到同一个 GridColumnInfo 类。  */
        fun createCacheKey(gridColumnBind: GridColumnBind, kMutableProperty: KMutableProperty1<*,*>): String {
            /* GridInfo 不仅仅需要记录注解信息，还需要使用字段的hash值联合作为唯一标识符，才能唯一识别一个注解。
             * 所以这里在最后边加了一句  kMutableProperty.hashCode() 。共同作为唯一标识符。*/
            return "${gridColumnBind.headerText}_${gridColumnBind.pattern}_${gridColumnBind.valueType}_${gridColumnBind.uiIgnore}_${gridColumnBind.columnWidth}_${kMutableProperty.hashCode()}"
        }
        /** 清理无效的缓存引用 */
        @Suppress("UNUSED")
        fun cleanup() {
            cache.entries.removeAll { (_, ref) -> ref.get() == null }
        }
        /** 清理 */
        @Suppress("UNUSED")
        fun clear() {
            cache.clear()
        }
    }
}
