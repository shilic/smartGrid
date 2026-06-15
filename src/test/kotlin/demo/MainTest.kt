package demo

import io.github.shilic.smartGrid.core.GridReader
import io.github.shilic.smartGrid.core.poiWorkbook
import demo.dataModel.dbcDataModel.CanProtocol
import demo.dataModel.udsDataModel.Ecu
import io.github.shilic.smartGrid.utils.toGsonString
import kotlin.test.Test

fun main() {
    println("Hello World!")
}
class MainTest {
    @Test
    fun udsDataTest() {
        println("-------------- 测试诊断数据 ------------")
        // 路径使用相对路径
        val filePath = "src/test/resources/excel/诊断规范调查表-模版.xlsx"
        // 使用路径实例化一个 workbook 并解析出数据。如果你想使用其他表格组件，使用适配器模式，让新组件实现 Workbook 系列接口即可实现任意表格的解析，不局限于EXCEL表格。
        val ecus = GridReader(poiWorkbook(filePath)).read(Ecu::class)
        println("ecus: ${ecus.toGsonString()}")
    }
    @Test
    fun dbcDataTest(){
        println("-------------- 测试整车DBC协议 ------------")
        // 路径使用相对路径
        val filePath = "src/test/resources/excel/DBC模版.xlsx"
        // 使用路径实例化一个 workbook 并解析出数据。如果你想使用其他表格组件，使用适配器模式，让新组件实现 Workbook 系列接口即可实现任意表格的解析，不局限于EXCEL表格。
        val canProtocols = GridReader(poiWorkbook(filePath)).read(CanProtocol::class)
        println("canProtocols: ${canProtocols.toGsonString()}")
    }
}
