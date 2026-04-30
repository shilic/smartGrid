package demo

import core.GridParser
import core.poiWorkbook
import demo.dataModel.Ecu
import utils.toGsonString
import kotlin.test.Test

fun main() {
    println("Hello World!")
}
class MainTest {
    @Test
    fun test1() {
        println("test1")
        // 路径使用相对路径
        val filePath = "src/test/resources/excel/诊断规范调查表-模版.xlsx"
        // 使用路径实例化一个 workbook 并解析出数据。如果你想使用其他表格组件，使用适配器模式，让新组件实现 Workbook 系列接口即可。
        val ecus = GridParser(poiWorkbook(filePath)).parse(Ecu::class)
        println("ecus: ${ecus.toGsonString()}")
    }
}
