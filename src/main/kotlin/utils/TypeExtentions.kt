package person.shilicheng.utils

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

/**
 * 获取字典类型的键和值类型
 * 使用 Result 类型替代 out 参数，提供更安全的错误处理
 */
fun KType.getDictionaryKeyValueTypes(): Pair<KType, KType> {
    require(this.isDictionaryType()) { "传入的类型不是字典类型，无法解析键和值的类型。" }

    val arguments = this.arguments
    require(arguments.size == 2) { "字典类型应包含两个类型参数" }

    val keyType = arguments[0].type ?: throw IllegalArgumentException("无法获取键类型")
    val valueType = arguments[1].type ?: throw IllegalArgumentException("无法获取值类型")

    return keyType to valueType
}

/**  判断是否为字典类型 */
fun KType.isDictionaryType(): Boolean {
    val classifier = this.classifier as? KClass<*> ?: return false
    // 使用 isSubclassOf
    return classifier.isSubclassOf(Map::class)
}
