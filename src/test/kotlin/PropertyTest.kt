import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.KProperty

class PropertyTest {
    class MyClass {
        val instanceProperty: String = "test"
    }
    @Test
    fun test2() {
        val obj1 = MyClass()
        val obj2 = MyClass()
        val obj3 = MyClass()

        val prop1 : KProperty<*> = obj1::instanceProperty
        val prop2 : KProperty<*> = obj2::instanceProperty
        val prop1a1 : KProperty<*> = obj1::instanceProperty
        val prop3 : KProperty<*> = obj3::instanceProperty

        // 可能是 false（不同实例）
        println("obj1.hashCode() == obj2.hashCode() = ${prop1.hashCode() == prop2.hashCode()}")
        // 通常是 true（相同实例）
        println("obj1.hashCode() == obj1.hashCode() = ${prop1.hashCode() == prop1a1.hashCode()}")
        println("obj2.hashCode() == obj3.hashCode() = ${prop2.hashCode() == prop3.hashCode()}")
        assertEquals(prop1.hashCode(), prop2.hashCode())
        assertEquals(prop1.hashCode(), prop1a1.hashCode())
        assertEquals(prop1.hashCode(), prop3.hashCode())
        assertEquals(prop2.hashCode(), prop3.hashCode())
    }
}