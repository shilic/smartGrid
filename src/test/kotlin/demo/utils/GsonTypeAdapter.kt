package demo.utils

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.lang.reflect.ParameterizedType

/**
 * 简洁的十六进制格式化工具，所有整数类型都转换为带0x前缀的十六进制字符串
 */
object GsonAdapter {
    // 缓存的 Gson 实例
    private var gsonInstance: Gson? = null

    /**
     * 获取配置了十六进制序列化的 Gson 实例
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    val gson: Gson
        get() = gsonInstance ?: GsonBuilder()
            .registerTypeHierarchyAdapter(Number::class.java, NumberHexAdapter())

            // 无符号类型适配器
            .registerTypeAdapter(UInt::class.java, UIntAdapter())
            .registerTypeAdapter(ULong::class.java, ULongAdapter())
            .registerTypeAdapter(UShort::class.java, UShortAdapter())
            .registerTypeAdapter(UByte::class.java, UByteAdapter())

            // 有符号基本类型数组
            .registerTypeHierarchyAdapter(ByteArray::class.java, ByteArrayHexAdapter())
            .registerTypeHierarchyAdapter(IntArray::class.java, IntArrayHexAdapter())
            .registerTypeHierarchyAdapter(LongArray::class.java, LongArrayHexAdapter())
            .registerTypeHierarchyAdapter(ShortArray::class.java, ShortArrayHexAdapter())

            // 无符号基本类型数组
            .registerTypeAdapter(UByteArray::class.java, UByteArrayHexAdapter())
            .registerTypeAdapter(UIntArray::class.java, UIntArrayHexAdapter())
            .registerTypeAdapter(ULongArray::class.java, ULongArrayHexAdapter())
            .registerTypeAdapter(UShortArray::class.java, UShortArrayHexAdapter())

            // 有符号对象数组
            .registerTypeHierarchyAdapter(Array<Int>::class.java, IntArrayObjectAdapter())
            .registerTypeHierarchyAdapter(Array<Long>::class.java, LongArrayObjectAdapter())
            .registerTypeHierarchyAdapter(Array<Short>::class.java, ShortArrayObjectAdapter())
            .registerTypeHierarchyAdapter(Array<Byte>::class.java, ByteArrayObjectAdapter())

            // 无符号对象数组适配器工厂
            .registerTypeHierarchyAdapter(Array<UInt>::class.java, UIntObjectArrayAdapter())
            .registerTypeHierarchyAdapter(Array<ULong>::class.java, ULongObjectArrayAdapter())
            .registerTypeHierarchyAdapter(Array<UShort>::class.java, UShortObjectArrayAdapter())
            .registerTypeHierarchyAdapter(Array<UByte>::class.java, UByteObjectArrayAdapter())

            .registerTypeAdapterFactory(ListHexAdapterFactory())
            .registerTypeAdapterFactory(SetHexAdapterFactory())
            .setPrettyPrinting()              // 美观打印，每个字段换一行
            .disableHtmlEscaping()           // 禁用HTML转义
            .serializeNulls()                // 序列化null值
            .create()
            .also { gsonInstance = it }
}

/**
 * 通用数字类型适配器 - 将整数转换为0x前缀的十六进制
 */
private class NumberHexAdapter : JsonSerializer<Number>, JsonDeserializer<Number> {

    override fun serialize(
        src: Number?,
        typeOfSrc: java.lang.reflect.Type?,
        context: JsonSerializationContext
    ): JsonElement {
        return when (src) {
            null -> JsonNull.INSTANCE
            is Int -> JsonPrimitive("0x${src.toUInt().toString(16).uppercase()}")
            is Long -> JsonPrimitive("0x${src.toULong().toString(16).uppercase()}")
            is Short -> JsonPrimitive("0x${src.toUShort().toString(16).uppercase()}")
            is Byte -> JsonPrimitive("0x${src.toUByte().toString(16).uppercase()}")
            else -> JsonPrimitive(src) // Float, Double 保持原样
        }
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: java.lang.reflect.Type?,
        context: JsonDeserializationContext
    ): Number? {
        if (json == null || json.isJsonNull) return null

        val str = json.asString.trim()
        val hexStr = when {
            str.startsWith("0x", ignoreCase = true) -> str.substring(2)
            str.startsWith("#") -> str.substring(1)
            else -> str
        }

        val value = hexStr.toLongOrNull(16) ?: return null

        return when (typeOfT) {
            Int::class.java, Int::class.javaPrimitiveType -> value.toInt()
            Long::class.java, Long::class.javaPrimitiveType -> value
            Short::class.java, Short::class.javaPrimitiveType -> value.toShort()
            Byte::class.java, Byte::class.javaPrimitiveType -> value.toByte()
            else -> value
        }
    }
}

/**
 * 无符号整数类型适配器
 */
private class UIntAdapter : JsonSerializer<UInt>, JsonDeserializer<UInt> {
    override fun serialize(src: UInt?, typeOfSrc: java.lang.reflect.Type?, context: JsonSerializationContext): JsonElement {
        return src?.let { JsonPrimitive("0x${it.toString(16).uppercase()}") } ?: JsonNull.INSTANCE
    }

    override fun deserialize(json: JsonElement?, typeOfT: java.lang.reflect.Type?, context: JsonDeserializationContext): UInt? {
        if (json == null || json.isJsonNull) return null
        val str = json.asString.trim()
        val hexStr = when {
            str.startsWith("0x", ignoreCase = true) -> str.substring(2)
            str.startsWith("#") -> str.substring(1)
            else -> str
        }
        return hexStr.toUIntOrNull(16)
    }
}
private class ULongAdapter : JsonSerializer<ULong>, JsonDeserializer<ULong> {
    override fun serialize(src: ULong?, typeOfSrc: java.lang.reflect.Type?, context: JsonSerializationContext): JsonElement {
        return src?.let { JsonPrimitive("0x${it.toString(16).uppercase()}") } ?: JsonNull.INSTANCE
    }

    override fun deserialize(json: JsonElement?, typeOfT: java.lang.reflect.Type?, context: JsonDeserializationContext): ULong? {
        if (json == null || json.isJsonNull) return null
        val str = json.asString.trim()
        val hexStr = when {
            str.startsWith("0x", ignoreCase = true) -> str.substring(2)
            str.startsWith("#") -> str.substring(1)
            else -> str
        }
        return hexStr.toULongOrNull(16)
    }
}
private class UShortAdapter : JsonSerializer<UShort>, JsonDeserializer<UShort> {
    override fun serialize(src: UShort?, typeOfSrc: java.lang.reflect.Type?, context: JsonSerializationContext): JsonElement {
        return src?.let { JsonPrimitive("0x${it.toString(16).uppercase()}") } ?: JsonNull.INSTANCE
    }

    override fun deserialize(json: JsonElement?, typeOfT: java.lang.reflect.Type?, context: JsonDeserializationContext): UShort? {
        if (json == null || json.isJsonNull) return null
        val str = json.asString.trim()
        val hexStr = when {
            str.startsWith("0x", ignoreCase = true) -> str.substring(2)
            str.startsWith("#") -> str.substring(1)
            else -> str
        }
        return hexStr.toUShortOrNull(16)
    }
}
private class UByteAdapter : JsonSerializer<UByte>, JsonDeserializer<UByte> {
    override fun serialize(src: UByte?, typeOfSrc: java.lang.reflect.Type?, context: JsonSerializationContext): JsonElement {
        return src?.let { JsonPrimitive("0x${it.toString(16).uppercase()}") } ?: JsonNull.INSTANCE
    }

    override fun deserialize(json: JsonElement?, typeOfT: java.lang.reflect.Type?, context: JsonDeserializationContext): UByte? {
        if (json == null || json.isJsonNull) return null
        val str = json.asString.trim()
        val hexStr = when {
            str.startsWith("0x", ignoreCase = true) -> str.substring(2)
            str.startsWith("#") -> str.substring(1)
            else -> str
        }
        return hexStr.toUByteOrNull(16)
    }
}


/**
 * 基本类型数组适配器基类
 */
private abstract class PrimitiveArrayHexAdapter<T> : TypeAdapter<T>() {

    protected abstract fun toHexArray(array: T): JsonArray
    protected abstract fun fromJsonArray(jsonArray: JsonArray): T

    override fun write(writer: JsonWriter, value: T?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        val jsonArray = toHexArray(value)
        GsonAdapter.gson.toJson(jsonArray, writer)
    }

    override fun read(reader: JsonReader): T? {
        val jsonArray = JsonParser.parseReader(reader).asJsonArray
        return fromJsonArray(jsonArray)
    }
}

/**
 * 各种基本类型数组的具体实现
 */
private class IntArrayHexAdapter : PrimitiveArrayHexAdapter<IntArray>() {
    override fun toHexArray(array: IntArray) = JsonArray().apply {
        array.forEach { add("0x${it.toUInt().toString(16).uppercase()}") }
    }
    override fun fromJsonArray(jsonArray: JsonArray) =
        IntArray(jsonArray.size()) { jsonArray[it].asString.substring(2).toInt(16) }
}
private class LongArrayHexAdapter : PrimitiveArrayHexAdapter<LongArray>() {
    override fun toHexArray(array: LongArray) = JsonArray().apply {
        array.forEach { add("0x${it.toULong().toString(16).uppercase()}") }
    }
    override fun fromJsonArray(jsonArray: JsonArray) =
        LongArray(jsonArray.size()) { jsonArray[it].asString.substring(2).toLong(16) }
}
private class ShortArrayHexAdapter : PrimitiveArrayHexAdapter<ShortArray>() {
    override fun toHexArray(array: ShortArray) = JsonArray().apply {
        array.forEach { add("0x${it.toUShort().toString(16).uppercase()}") }
    }
    override fun fromJsonArray(jsonArray: JsonArray) =
        ShortArray(jsonArray.size()) { jsonArray[it].asString.substring(2).toShort(16) }
}
private class ByteArrayHexAdapter : PrimitiveArrayHexAdapter<ByteArray>() {
    override fun toHexArray(array: ByteArray) = JsonArray().apply {
        array.forEach { add("0x${it.toUByte().toString(16).uppercase()}") }
    }
    override fun fromJsonArray(jsonArray: JsonArray) =
        ByteArray(jsonArray.size()) { jsonArray[it].asString.substring(2).toByte(16) }
}
/**
 * 无符号基本类型数组适配器
 */
@OptIn(ExperimentalUnsignedTypes::class)
private class UIntArrayHexAdapter : TypeAdapter<UIntArray>() {
    override fun write(writer: JsonWriter, value: UIntArray?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginArray()
        value.forEach { writer.value("0x${it.toString(16).uppercase()}") }
        writer.endArray()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun read(reader: JsonReader): UIntArray? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        val list = mutableListOf<UInt>()
        reader.beginArray()
        while (reader.hasNext()) {
            val hexStr = reader.nextString().substring(2)
            list.add(hexStr.toUInt(16))
        }
        reader.endArray()
        return list.toUIntArray()
    }
}
@OptIn(ExperimentalUnsignedTypes::class)
private class ULongArrayHexAdapter : TypeAdapter<ULongArray>() {
    override fun write(writer: JsonWriter, value: ULongArray?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginArray()
        value.forEach { writer.value("0x${it.toString(16).uppercase()}") }
        writer.endArray()
    }

    override fun read(reader: JsonReader): ULongArray? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        val list = mutableListOf<ULong>()
        reader.beginArray()
        while (reader.hasNext()) {
            val hexStr = reader.nextString().substring(2)
            list.add(hexStr.toULong(16))
        }
        reader.endArray()
        return list.toULongArray()
    }
}
@OptIn(ExperimentalUnsignedTypes::class)
private class UShortArrayHexAdapter : TypeAdapter<UShortArray>() {
    override fun write(writer: JsonWriter, value: UShortArray?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginArray()
        value.forEach { writer.value("0x${it.toString(16).uppercase()}") }
        writer.endArray()
    }

    override fun read(reader: JsonReader): UShortArray? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        val list = mutableListOf<UShort>()
        reader.beginArray()
        while (reader.hasNext()) {
            val hexStr = reader.nextString().substring(2)
            list.add(hexStr.toUShort(16))
        }
        reader.endArray()
        return list.toUShortArray()
    }
}
@OptIn(ExperimentalUnsignedTypes::class)
private class UByteArrayHexAdapter : TypeAdapter<UByteArray>() {
    override fun write(writer: JsonWriter, value: UByteArray?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginArray()
        value.forEach { writer.value("0x${it.toString(16).uppercase()}") }
        writer.endArray()
    }

    override fun read(reader: JsonReader): UByteArray? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        val list = mutableListOf<UByte>()
        reader.beginArray()
        while (reader.hasNext()) {
            val hexStr = reader.nextString().substring(2)
            list.add(hexStr.toUByte(16))
        }
        reader.endArray()
        return list.toUByteArray()
    }
}
/**
 * 对象数组适配器
 */
private class IntArrayObjectAdapter : PrimitiveArrayHexAdapter<Array<Int>>() {
    override fun toHexArray(array: Array<Int>) = JsonArray().apply {
        array.forEach { add("0x${it.toUInt().toString(16).uppercase()}") }
    }
    override fun fromJsonArray(jsonArray: JsonArray) =
        Array(jsonArray.size()) { jsonArray[it].asString.substring(2).toInt(16) }
}
private class LongArrayObjectAdapter : PrimitiveArrayHexAdapter<Array<Long>>() {
    override fun toHexArray(array: Array<Long>) = JsonArray().apply {
        array.forEach { add("0x${it.toULong().toString(16).uppercase()}") }
    }
    override fun fromJsonArray(jsonArray: JsonArray) =
        Array(jsonArray.size()) { jsonArray[it].asString.substring(2).toLong(16) }
}
private class ShortArrayObjectAdapter : PrimitiveArrayHexAdapter<Array<Short>>() {
    override fun toHexArray(array: Array<Short>) = JsonArray().apply {
        array.forEach { add("0x${it.toUShort().toString(16).uppercase()}") }
    }
    override fun fromJsonArray(jsonArray: JsonArray) =
        Array(jsonArray.size()) { jsonArray[it].asString.substring(2).toShort(16) }
}
private class ByteArrayObjectAdapter : PrimitiveArrayHexAdapter<Array<Byte>>() {
    override fun toHexArray(array: Array<Byte>) = JsonArray().apply {
        array.forEach { add("0x${it.toUByte().toString(16).uppercase()}") }
    }
    override fun fromJsonArray(jsonArray: JsonArray) =
        Array(jsonArray.size()) { jsonArray[it].asString.substring(2).toByte(16) }
}

private class UIntObjectArrayAdapter : TypeAdapter<Array<UInt>>() {
    override fun write(writer: JsonWriter, value: Array<UInt>?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginArray()
        value.forEach { writer.value("0x${it.toString(16).uppercase()}") }
        writer.endArray()
    }

    override fun read(reader: JsonReader): Array<UInt>? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        val list = mutableListOf<UInt>()
        reader.beginArray()
        while (reader.hasNext()) {
            val hexStr = reader.nextString().substring(2)
            list.add(hexStr.toUInt(16))
        }
        reader.endArray()
        return list.toTypedArray()
    }
}
private class ULongObjectArrayAdapter : TypeAdapter<Array<ULong>>() {
    override fun write(writer: JsonWriter, value: Array<ULong>?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginArray()
        value.forEach { writer.value("0x${it.toString(16).uppercase()}") }
        writer.endArray()
    }

    override fun read(reader: JsonReader): Array<ULong>? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        val list = mutableListOf<ULong>()
        reader.beginArray()
        while (reader.hasNext()) {
            val hexStr = reader.nextString().substring(2)
            list.add(hexStr.toULong(16))
        }
        reader.endArray()
        return list.toTypedArray()
    }
}
private class UShortObjectArrayAdapter : TypeAdapter<Array<UShort>>() {
    override fun write(writer: JsonWriter, value: Array<UShort>?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginArray()
        value.forEach { writer.value("0x${it.toString(16).uppercase()}") }
        writer.endArray()
    }

    override fun read(reader: JsonReader): Array<UShort>? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        val list = mutableListOf<UShort>()
        reader.beginArray()
        while (reader.hasNext()) {
            val hexStr = reader.nextString().substring(2)
            list.add(hexStr.toUShort(16))
        }
        reader.endArray()
        return list.toTypedArray()
    }
}
private class UByteObjectArrayAdapter : TypeAdapter<Array<UByte>>() {
    override fun write(writer: JsonWriter, value: Array<UByte>?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginArray()
        value.forEach { writer.value("0x${it.toString(16).uppercase()}") }
        writer.endArray()
    }

    override fun read(reader: JsonReader): Array<UByte>? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        val list = mutableListOf<UByte>()
        reader.beginArray()
        while (reader.hasNext()) {
            val hexStr = reader.nextString().substring(2)
            list.add(hexStr.toUByte(16))
        }
        reader.endArray()
        return list.toTypedArray()
    }
}
/**
 * List 适配器工厂
 */
@Suppress("UNCHECKED_CAST")
private class ListHexAdapterFactory : TypeAdapterFactory {

    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!List::class.java.isAssignableFrom(type.rawType)) return null
        if (type.type !is ParameterizedType) return null

        val elementType = (type.type as ParameterizedType).actualTypeArguments[0]

        return when (elementType) {
            Int::class.java -> ListHexAdapter<Int>(Int::class.java) as TypeAdapter<T>
            Long::class.java -> ListHexAdapter<Long>(Long::class.java) as TypeAdapter<T>
            Short::class.java -> ListHexAdapter<Short>(Short::class.java) as TypeAdapter<T>
            Byte::class.java -> ListHexAdapter<Byte>(Byte::class.java) as TypeAdapter<T>
            else -> null
        }
    }
}

/**
 * Set 适配器工厂
 */
@Suppress("UNCHECKED_CAST")
private class SetHexAdapterFactory : TypeAdapterFactory {

    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!Set::class.java.isAssignableFrom(type.rawType)) return null
        if (type.type !is ParameterizedType) return null

        val elementType = (type.type as ParameterizedType).actualTypeArguments[0]

        return when (elementType) {
            Int::class.java -> SetHexAdapter<Int>(Int::class.java) as TypeAdapter<T>
            Long::class.java -> SetHexAdapter<Long>(Long::class.java) as TypeAdapter<T>
            Short::class.java -> SetHexAdapter<Short>(Short::class.java) as TypeAdapter<T>
            Byte::class.java -> SetHexAdapter<Byte>(Byte::class.java) as TypeAdapter<T>
            else -> null
        }
    }
}

/**
 * 泛型 List 适配器
 */
@Suppress("UNCHECKED_CAST")
private class ListHexAdapter<T : Number>(private val elementClass: Class<T>) : TypeAdapter<List<T>>() {

    override fun write(writer: JsonWriter, value: List<T>?) {
        if (value == null) {
            writer.nullValue()
            return
        }

        writer.beginArray()
        value.forEach { number ->
            val hexStr = when (number) {
                is Int -> "0x${number.toUInt().toString(16).uppercase()}"
                is Long -> "0x${number.toULong().toString(16).uppercase()}"
                is Short -> "0x${number.toUShort().toString(16).uppercase()}"
                is Byte -> "0x${number.toUByte().toString(16).uppercase()}"
                else -> number.toString()
            }
            writer.value(hexStr)
        }
        writer.endArray()
    }

    override fun read(reader: JsonReader): List<T>? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }

        val list = mutableListOf<T>()
        reader.beginArray()
        while (reader.hasNext()) {
            val hexStr = reader.nextString().substring(2)
            val value = when (elementClass) {
                Int::class.java -> hexStr.toInt(16) as T
                Long::class.java -> hexStr.toLong(16) as T
                Short::class.java -> hexStr.toShort(16) as T
                Byte::class.java -> hexStr.toByte(16) as T
                else -> throw IllegalArgumentException("Unsupported type: $elementClass")
            }
            list.add(value)
        }
        reader.endArray()
        return list
    }
}

/**
 * 泛型 Set 适配器
 */
@Suppress("UNCHECKED_CAST")
private class SetHexAdapter<T : Number>(private val elementClass: Class<T>) : TypeAdapter<Set<T>>() {

    override fun write(writer: JsonWriter, value: Set<T>?) {
        if (value == null) {
            writer.nullValue()
            return
        }

        writer.beginArray()
        value.forEach { number ->
            val hexStr = when (number) {
                is Int -> "0x${number.toUInt().toString(16).uppercase()}"
                is Long -> "0x${number.toULong().toString(16).uppercase()}"
                is Short -> "0x${number.toUShort().toString(16).uppercase()}"
                is Byte -> "0x${number.toUByte().toString(16).uppercase()}"
                else -> number.toString()
            }
            writer.value(hexStr)
        }
        writer.endArray()
    }

    override fun read(reader: JsonReader): Set<T>? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }

        val set = mutableSetOf<T>()
        reader.beginArray()
        while (reader.hasNext()) {
            val hexStr = reader.nextString().substring(2)
            val value = when (elementClass) {
                Int::class.java -> hexStr.toInt(16) as T
                Long::class.java -> hexStr.toLong(16) as T
                Short::class.java -> hexStr.toShort(16) as T
                Byte::class.java -> hexStr.toByte(16) as T
                else -> throw IllegalArgumentException("Unsupported type: $elementClass")
            }
            set.add(value)
        }
        reader.endArray()
        return set
    }
}
/** 仅在测试时使用，如果你不喜欢，可以使用其他序列化框架 */
inline fun <reified T> T.toGsonString(): String = GsonAdapter.gson.toJson(this)

inline fun <reified T> String.fromGsonString(): T = GsonAdapter.gson.fromJson(this, T::class.java)