package person.shilicheng.utils

import kotlin.reflect.KClass

/** 动态字典，支持在运行时确定键值类型 */
fun <K : Any, V : Any> createTypedMap(keyType: KClass<K>, valueType: KClass<V>): MutableMap<K, V> {
    return HashMap<K, V>()
}