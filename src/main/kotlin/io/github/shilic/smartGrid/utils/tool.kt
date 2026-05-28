package io.github.shilic.smartGrid.utils

import io.github.shilic.smartGrid.core.GridColumnBind
import io.github.shilic.smartGrid.core.GridColumnInfo
import io.github.shilic.smartGrid.core.GridValueType
import io.github.shilic.smartGrid.core.bindTo
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/** 缓存反射信息。
 *
 * 使用类型作为键， 使用泛型信息类的集合保存这个类的所有
 * */
private val mBindCache : MutableMap<KClass<*>, List<GridColumnInfo>> = mutableMapOf()

/** 从缓存中获取反射信息，或者重新使用反射获取。
 *
 * 默认 筛选注解的 pattern(不为空) 和 valueType ；
 * */
fun KClass<*>.getOrCacheBinds() : List<GridColumnInfo> {
    return mBindCache.getOrPut(this) { this.getGridInfos() }
}
/**
 * 使用反射, 从类型上的所有属性获取绑定的 GridInfo 信息。
 *
 * 默认 筛选注解的 pattern(不为空) 和 valueType ；
 *
 * 你可能需要缓存反射的结果。
 *
 * @param filter 是否使用默认过滤条件（pattern 非空 或 valueType = OtherPage）; 当你要获取所有注解时，使 defaultFilter 为 false ;
 */
fun KClass<*>.getGridInfos(filter: Boolean = true): List<GridColumnInfo> =
    // 获取按顺序定义的所有字段
    getOrderProperties()
        // 步骤1: 创建GridBind注解(筛选注解不为空)和属性的键值对集合
        .mapNotNull { property -> property.findAnnotation<GridColumnBind>()?.let { property to it } }
        // 步骤2: 使用kotlin的函数筛选注解的 pattern(不为空) 和 valueType 。(filter 为 false 时，立刻放弃筛选(全过滤)。)
        .filter { (_, bind) -> !filter || bind.pattern.isNotBlank() || bind.valueType == GridValueType.OtherSheet || bind.valueType == GridValueType.SpecificSheet  }
        // 步骤3: 筛选可变属性
        .filter { (property, _) ->
            val isMutable = property is KMutableProperty1<*, *>
            // 当默认过滤开启, 且属性不可变时，抛异常
            if (!isMutable && filter) {
                throw IllegalArgumentException("类型\"${this.simpleName}\"上的属性\"${property.name}\"必须为可变属性; " +
                        "因为标注了\"${GridColumnBind::class.simpleName}\"注解的属性需要通过反射赋值; 请将属性声明修改为 `var ${property.name}`。")
            }
            isMutable
        }
        // 步骤4: 将筛选出来的值使用bindTo变成新的集合
        .map { (property, bind) -> bind.bindTo(property as KMutableProperty1<*, *>) }

/** 查找类下边的可变属性;
 *
 * 先按照定义顺序排序; 再进行过滤，过滤出可变属性。 */
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
 * 将 KClass<> 转换为通用的枚举类类型 KClass<out Enum<>>。
 *
 * 注意：返回的是通配符类型，表示"某个枚举类型"，但不知道具体是哪种枚举。并且如果不是枚举类型，则返回 null
 *
 * 这在你只需要枚举的通用操作（如获取 name、ordinal）时有用。
 * 但不能进行类型安全的特定枚举操作。
 */
fun KClass<*>.safeAsEnumClass(): KClass<out Enum<*>>? =
    // 检查类型是否是枚举类型，如果是则强制转换为枚举，不是则返回 null
    takeIf { it.java.isEnum && Enum::class.java.isAssignableFrom(it.java) }
        ?.let {
            // 由于类型擦除，这里需要不安全的转换,  但我们通过 isEnum 检查确保了安全
            @Suppress("UNCHECKED_CAST")
            it as KClass<out Enum<*>>
        }