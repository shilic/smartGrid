package person.shilicheng.core

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/** 从类型上获取绑定的 GridInfo 信息 */
fun KClass<*>.getGridInfos(): List<GridInfo> = memberProperties
    .mapNotNull { property ->
        property.findAnnotation<GridBind>()
            ?.takeIf { it.pattern.isNotEmpty() || it.valueType == GridValueType.OtherPage }
            ?.bindTo(property)
    }
/**  获取指定字段的指定特性 */
inline fun <reified T : Annotation> KClass<*>.getMemberAnnotation(memberName: String): T =
    getPropertyByName(memberName)?.getCustomAnnotationOrNull<T>() ?: throw IllegalArgumentException("类型 ${simpleName} 中没有找到字段 $memberName")
/** 获取指定字段的指定特性(可空) */
inline fun <reified T : Annotation> KClass<*>.getMemberAnnotationOrNull(memberName: String): T? =
    getPropertyByName(memberName)?.getCustomAnnotationOrNull<T>()
/** 为属性引用添加扩展函数版本 */
inline fun <reified T : Annotation> KProperty<*>.getAnnotation(): T? = annotations.filterIsInstance<T>().firstOrNull()
/**  获取类的成员变量(可空) */
fun KClass<*>.getPropertyByName(memberName: String): KProperty<*>? = memberProperties.find { it.name == memberName }
/**  获取成员变量的自定义注解 */
inline fun <reified T : Annotation> KProperty<*>.getCustomAnnotation(): T = annotations.filterIsInstance<T>().firstOrNull() ?: throw IllegalArgumentException("属性 $name 上没有找到 ${T::class.simpleName} 注解")
/**  获取成员变量的自定义注解（可空版本） */
inline fun <reified T : Annotation> KProperty<*>.getCustomAnnotationOrNull(): T? = annotations.filterIsInstance<T>().firstOrNull()