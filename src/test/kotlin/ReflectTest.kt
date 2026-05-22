import demo.utils.toGsonString
import io.github.shilic.smartGrid.utils.memberMutableProperties
import kotlin.reflect.KMutableProperty1
import kotlin.test.Test

class Class1{
    var textProperty: String = "初始值"
    var uIntProperty: UInt = 0u
}

class Class1Test{
    @Test
    fun test1(){
        val class1 = Class1()
        println("初始值 = ${class1.toGsonString()}")
        val textKMutableProperty1 : KMutableProperty1<*,*> = Class1::class.memberMutableProperties.first { it.name == Class1::textProperty.name }
        textKMutableProperty1.setter.call(class1, "新值")
        val uIntKMutableProperty1 : KMutableProperty1<*,*> = Class1::class.memberMutableProperties.first { it.name == Class1::uIntProperty.name }
        // 这里如果直接写入 整形值，会被视为 Int 导致报错，需要写入无符号值。或者使用 toUInt() 转换。
        uIntKMutableProperty1.setter.call(class1, 18895.toUInt())

        println("更改后的值 = ${class1.toGsonString()}")
    }
}