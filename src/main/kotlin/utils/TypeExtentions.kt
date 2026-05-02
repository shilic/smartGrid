package utils

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

/**  获取字典类型的键和值类型 */
fun KProperty1<*,*>.getDictionaryKeyValueTypes(): Pair<KClass<*>, KClass<*>>{
    require(isDictionaryType()){ "字段 `${this}` 数据类型错误, 必须是一个字典类型的数据"  }
    val arguments = this.returnType.arguments
    require(arguments.size == 2) { "字段 `${this}` 数据类型错误, 字典类型应包含两个类型参数" }

    val keyType = arguments[0].type ?: throw IllegalArgumentException("字段 `${this}` 数据类型错误, 无法获取键类型")
    val valueType = arguments[1].type ?: throw IllegalArgumentException("字段 `${this}` 数据类型错误, 无法获取值类型")

    return keyType.classifier as KClass<*> to valueType.classifier as KClass<*>
}
/**  获取字典类型的键和值类型 */
fun KType.getDictionaryKeyValueTypes(): Pair<KClass<*>, KClass<*>> {
    require(this.isDictionaryType()) { "传入的类型不是字典类型，无法解析键和值的类型。" }
    val arguments = this.arguments
    require(arguments.size == 2) { "字典类型应包含两个类型参数" }

    val keyType = arguments[0].type ?: throw IllegalArgumentException("无法获取键类型")
    val valueType = arguments[1].type ?: throw IllegalArgumentException("无法获取值类型")

    return keyType.classifier as KClass<*> to valueType.classifier as KClass<*>
}
/**  判断是否为字典类型 */
fun KProperty1<*,*>.isDictionaryType(): Boolean {
    return this.returnType.isDictionaryType()
}
/**  判断是否为字典类型 */
fun KType.isDictionaryType(): Boolean {
    val classifier = this.classifier as? KClass<*> ?: return false
    // 使用 isSubclassOf
    return classifier.isSubclassOf(Map::class)
}
