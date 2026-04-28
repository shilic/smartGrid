package demo

import core.GridParser
import demo.dataModel.Ecu
import demo.utils.toJsonString
import kotlin.test.Test

fun main() {
    println("Hello World!")
}
class MainTest {
    @Test
    fun test1() {
        println("test1")
        val filePath = "src/test/resources/excel/诊断规范调查表-模版.xlsx"
        val ecus = GridParser(filePath).parse(Ecu::class)
        println("ecus: ${ecus.toJsonString()}")
    }
}
