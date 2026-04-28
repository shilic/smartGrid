package utils

import core.GridBind
import core.GridInfo
import core.GridValueType
import core.bindTo
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/** 动态字典，支持在运行时确定键值类型 */
fun <K : Any, V : Any> createTypedMap(keyType: KClass<K>, valueType: KClass<V>): MutableMap<K, V> {
    return HashMap()
}
/**
 * 使用反射, 从类型上的所有属性获取绑定的 GridInfo 信息
 *
 * @param filter 自定义过滤条件，为 null 时不进行过滤
 * @param defaultFilter 是否使用默认过滤条件（pattern 非空 或 valueType = OtherPage）; 当你要获取所有注解时，使 defaultFilter 为 false ;
 *
 * 注意：当同时提供 filter 时，defaultFilter 会被忽略;
 */
fun KClass<*>.getGridInfos(filter: ((GridBind) -> Boolean)? = null, defaultFilter: Boolean = true): List<GridInfo> = memberMutableProperties
    .mapNotNull { kMutableProperty -> kMutableProperty.findAnnotation<GridBind>()
        ?.let { annotation ->
            when {
                // 1. 优先使用自定义过滤条件
                filter != null -> annotation.takeIf(filter)
                // 2. 使用默认过滤条件 (当同时提供 filter 时，defaultFilter 会被忽略)
                /* 规则1 : 如果 正则表达式 pattern 那一项没有值，则忽略这一项。
                * 例如某些值，你只是填写了列标题，只是想在UI界面进行展示，而不需要从 excel 表格进行识别。
                * 使用 takeIf { it.pattern.isNotBlank() } 过滤出 pattern 非空的值。
                *
                * 规则2 : 如果该字段的数据来源是其他页面, valueType = OtherPage ，则忽略 pattern 的空值检查。
                * (关键逻辑) (valueType 标记为 OtherPage 时，不需要填写 pattern)
                * 这个时候添加正则表达式没用，因为数据不在当前表格, 就算填写了 pattern 之后, 也无法从当前表格获取列下标, 只有到另外的表格进行查询。
                * 故需要筛选出全部的 valueType = OtherPage 的注解。
                *
                * 也就是说，需要筛选出 pattern 有值，或者 valueType = OtherPage 的注解。  */
                defaultFilter -> annotation.takeIf { it.pattern.isNotBlank() || it.valueType == GridValueType.OtherPage }
                // 3. 不过滤，返回所有注解
                else -> annotation
            }?.bindTo(kMutableProperty)
        }
    }
/** 查找类下边的可变属性; 先按照定义顺序排序; 再进行过滤，过滤出可变属性。 */
val <T : Any> KClass<T>.memberMutableProperties: List<KMutableProperty1<*,*>>
    get() = getOrderProperties().filterIsInstance<KMutableProperty1<*,*>>()
/**
 * 按照属性在 Kotlin 源代码中的定义顺序获取成员属性
 *
 * 注意：Kotlin 反射的默认顺序是未定义的，此函数通过 Java 反射获取字段定义顺序
 * 来模拟 Kotlin 源码中的属性顺序。对于大多数情况这是可靠的，但某些编译器优化
 * 可能会重新排序字段。
 */
fun <T : Any> KClass<T>.getOrderProperties(): List<KProperty1<T, *>> {
    if (memberProperties.isEmpty()) return emptyList()
    // 获取所有 Java 字段（包括私有的）
    val fieldOrderMap = this.java.declaredFields
        // 过滤掉编译器生成的字段，如 $annotations, $delegate 等
        .filter { field -> !field.name.startsWith("$") }
        // 使用 mapIndexed 同时获取索引和值; 创建映射：字段名 -> 字段位置
        .mapIndexed { index, field -> field.name to index }
        // 转换为不可变 Map
        .toMap()

    // 将 Kotlin 属性与 Java 字段关联并排序
    return memberProperties
        .map { property ->
            // 尝试获取属性的 Java 字段
            property.javaField?.also { it.isAccessible = true }
            property to property.javaField
        }
        .sortedWith(compareBy(
            // 首先按照 Java 字段的顺序排序
            { propertyWithField ->
                // 如果获取不到字段名，使用属性名
                val fieldName = propertyWithField.second?.name ?: propertyWithField.first.name
                fieldOrderMap[fieldName] ?: Int.MAX_VALUE
            },
            // 其次按照属性名排序（对于没有对应 Java 字段的情况）
            { propertyWithField -> propertyWithField.first.name }
        ))
        .map { it.first }
}
/**
 * 将 KClass<*> 转换为通用的枚举类类型 KClass<out Enum<*>>。
 * 注意：返回的是通配符类型，表示"某个枚举类型"，但不知道具体是哪种枚举。
 *
 * 这在你只需要枚举的通用操作（如获取 name、ordinal）时有用。
 * 但不能进行类型安全的特定枚举操作。
 */
fun KClass<*>.safeAsEnumClass(): KClass<out Enum<*>>? =
    takeIf { it.java.isEnum && Enum::class.java.isAssignableFrom(it.java) }
        ?.let {
            // 由于类型擦除，这里需要不安全的转换,  但我们通过 isEnum 检查确保了安全
            @Suppress("UNCHECKED_CAST")
            it as KClass<out Enum<*>>
        }