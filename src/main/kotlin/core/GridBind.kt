package person.shilicheng.core

import java.lang.ref.WeakReference
import kotlin.reflect.KMutableProperty1

/** 注解： 用于按字段绑定对象到表格 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class GridBind (
    /** headerText : 列标题(为空则直接忽略，不添加到表格，或者直接不添加本注解)。在UI上显示时，使用该属性。 */
    val headerText : String = "",
    /** 列标题的正则表达式。用于识别表头位置，以及嵌套识别枚举项。如果该属性没有值就表示不识别。*/
    val pattern : String = "",
    /** 绑定值的类型，字符串还是数值类型 */
    val valueType : GridValueType = GridValueType.NotDefined,
    /** UI层是否忽略，默认不忽略 */
    val uiIgnore : Boolean = false,
    /** 列宽 */
    val columnWidth : Int = 200,
)
/**  扩展函数，让GridBind可以直接绑定到 KMutableProperty1<*,*> */
fun GridBind.bindTo(property: KMutableProperty1<*,*>): GridInfo {
    return GridInfo.create(this, property)
}
/**  Grid 注解的扩展信息 */
class GridInfo private constructor (
    /** 记录列下标，可能为空，表示没有列下标 */
    var columnIndex: Int? = null,
    /** 持有一个 Grid 注解  */
    val gridBind: GridBind,
    /** 该注解和哪一个属性绑定 */
    val property: KMutableProperty1<*,*>,
    /** 如果该字段是一个子数据的键值对，还需要记录子数据的列序号和扩展信息键值对 */
    val subGridInfo: MutableList<GridInfo> = mutableListOf(),
    private val cacheKey : String,
) : IKey {
    override val key  get() = cacheKey

    /** 检查绑定信息是否有效，有效才会加入到UI界面中;
     * 不为空，有标题(用于添加表格列标题)，有数据类型(会根据数据类型添加对应格式的列，文本、数值或者下拉框), 并且没有被忽略(uiIgnore = false)。
     */
    val validity : Boolean get() = gridBind.headerText.isNotBlank() && gridBind.valueType != GridValueType.NotDefined   && !gridBind.uiIgnore
    companion object {
        /**  使用弱引用缓存，避免内存泄漏 */
        private val cache = mutableMapOf<String, WeakReference<GridInfo>>()
        /**  通过工厂创建实例 */
        fun create (gridBind: GridBind, property: KMutableProperty1<*,*>): GridInfo {
            return getOrCreate(gridBind, property)
        }
        /**  创建缓存键  */
        private fun createCacheKey(gridBind: GridBind, property: KMutableProperty1<*,*>): String {
            return "${gridBind.headerText}_${gridBind.pattern}_${gridBind.valueType}_${gridBind.uiIgnore}_${gridBind.columnWidth}_${property.hashCode()}"
        }
        /**  获取或创建GridInfo */
        private fun getOrCreate(gridBind: GridBind, property: KMutableProperty1<*,*>): GridInfo {
            val cacheKey = createCacheKey(gridBind, property)

            // 尝试从缓存中获取
            val cachedRef = cache[cacheKey]
            val cached = cachedRef?.get()

            return when {
                // 返回缓存实例
                cached != null -> cached
                else -> {
                    // 创建新实例并缓存
                    GridInfo(gridBind = gridBind, property = property, cacheKey = cacheKey).also { newInstance ->
                        cache[cacheKey] = WeakReference(newInstance)
                    }
                }
            }
        }
        // 清理无效的缓存引用
        fun cleanup() {
            cache.entries.removeAll { (_, ref) -> ref.get() == null }
        }
        // 获取缓存统计
        fun getStats(): String {
            cleanup()
            return "有效缓存: ${cache.size}"
        }
    }
}
