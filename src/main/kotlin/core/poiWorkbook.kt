package core

import org.apache.poi.ss.usermodel.Workbook
import utils.*

/** 返回一个由POI实现的表格组件。
 *
 * 表格组件接口，用于让框架适配所有类型的表格组件，故借用了POI的表格接口来实现功能。
 *
 * 你可以是一个 EXCEL 表格，也可以是一个 UI 层的 Grid 组件。
 * */
fun poiWorkbook(filePath: String): Workbook {
    val logTAG = "PoiWorkbook"
    require(filePath.isNotBlank()) { "$logTAG: 文件路径为空，无法识别文件。" }
    require(filePath.exists) { "$logTAG: 文件不存在，没找到指定文件: $filePath" }
    val fileExtension = filePath.fileExtension.lowercase()
    require(SUPPORTED_EXCEL_EXTENSIONS.contains(fileExtension)){"$logTAG: 不支持的文件格式: $fileExtension, 必须是\"xls\", \"xlsx\"文件"}
    return createWorkbook(filePath)
}
