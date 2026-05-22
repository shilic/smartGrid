package io.github.shilic.smartGrid.utils

import org.apache.poi.ss.usermodel.*
import io.github.shilic.smartGrid.exception.ExcelException
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Workbook 的相关扩展函数;
 * Kotlin 风格的工具类，使用扩展函数和顶层函数;
 */
private const val LOG_TAG = "ExcelTools"
val SUPPORTED_EXCEL_EXTENSIONS = listOf("xls", "xlsx")

/**
 * 获取一个工作簿;
 * 使用 Kotlin 的异常处理和资源管理;
 */
fun createWorkbook (filePath: String): Workbook {
    val fileExtension = filePath.fileExtension.lowercase()
    require(SUPPORTED_EXCEL_EXTENSIONS.contains(fileExtension)){"$LOG_TAG: 不支持的文件格式: $fileExtension, 必须是\"xls\", \"xlsx\"文件"}
    return try {
        // 使用 Kotlin 的 use 函数自动管理资源
        FileInputStream(filePath).use { fileStream -> WorkbookFactory.create(fileStream) }
    } catch (ex: FileNotFoundException) {
        throw ExcelException("$LOG_TAG: FileNotFoundException - 文件不存在: ${ex.message}")
    } catch (ex: IOException) {
        throw ExcelException("$LOG_TAG: IOException - 发生 IO 错误: ${ex.message}")
    } catch (ex: Exception) {
        throw ExcelException("$LOG_TAG: 未知错误: ${ex.message}")
    }
}

/**
 * 获取工作表名称到工作表的映射
 * 使用 Kotlin 的 associate 函数创建映射
 */
fun Workbook.getSheetMap(): Map<String, Sheet> = sheetIterator().asSequence().associateBy { it.sheetName }

/**  获取 Sheet 中最后一个包含实际数据的行索引（基于 0 开始） */
fun Sheet.getLastRowIndex(): Int  = when {
    // 条件1：工作表中没有行
    lastRowNum < 0 -> 0
    // 条件2：查找第一个有数据的行
    else -> (lastRowNum downTo 0)
    .firstOrNull { rowIndex ->
        getRow(rowIndex)?.isRowNonEmpty() == true
    } ?: 0
}

/**
 * 判断一行是否包含有效数据(非空); 只要一行至少有一个单元格有内容，返回真;
 * 使用 Kotlin 的作用域函数和 any 函数优化性能
 */
fun Row.isRowNonEmpty(): Boolean = iterator().asSequence().any { cell -> cell.isNotBlank() }

/**
 * 单元格不为空，并且有值
 */
fun Cell?.isNotBlank(): Boolean = when {
    // 这一段就是为了解决 java 和 kotlin 互操作的坑，而新加的一行代码。必须强行判断单元格非空。
    this == null -> false
    cellType.equals(CellType.BLANK) -> false
    else -> toString().isNotBlank()
}

/**
 * 将单元格转换为字符串值
 * 使用 Kotlin 的扩展属性，提供更自然的访问方式
 */
val Cell.stringValue: String
    get() = this.toString()

/**
 * 获取一行中最后一个有值单元格的列索引（基于 0）
 */
fun Row.getLastColumnIndex(): Int = when {
    lastCellNum <= 0 -> 0
    else -> {
        val lastPossibleIndex = lastCellNum - 1
        (lastPossibleIndex downTo 0)
            .firstOrNull { columnIndex ->
                getCell(columnIndex)?.isNotBlank() == true
            } ?: 0
    }
}

/**
 * 检查单元格是否有删除线
 */
fun Cell.isStrikeThrough(): Boolean {
    return try {
        val font = sheet.workbook.getFontAt(cellStyle.fontIndex)
        font.strikeout
    } catch (e: Exception) {
        // 如果发生异常（如索引越界），返回false
        false
    }
}
/**
 * 遍历工作表中所有单元格，并对单个单元格执行一个事件;
 * 如果事件返回 true，则终止循环;
 * 使用 inline 和 crossinline 优化性能;
 */
inline fun Sheet.forEachCell(crossinline action: (Cell) -> Boolean) {
    val lastRowIndex = getLastRowIndex()
    for (rowIndex in 0..lastRowIndex) {
        val row = getRow(rowIndex) ?: continue
        val lastColumnIndex = row.getLastColumnIndex()
        for (columnIndex in 0..lastColumnIndex) {
            val cell = row.getCell(columnIndex) ?: continue
            val shouldStop = action(cell)
            if (shouldStop) {
                return
            }
        }
    }
}
/**
 * 将一个单元格以分数格式解析;
 * 使用 Kotlin 的正则表达式 DSL 和异常处理;
 */
fun Cell.parseFraction(): Double {
    val input = stringValue.trim()
    println("解析分数: '$input'")
    val fractionRegex = """^\s*([-+]?\d*\.?\d+)\s*/\s*([-+]?\d*\.?\d+)\s*$""".toRegex()
    val matchResult = fractionRegex.matchEntire(input) ?: throw ExcelException("请确保输入是分数，错误单元格值: ${excelIndex()}, 单元格坐标: $exCell ")
    // 分子
    val numeratorStr = matchResult.groupValues[1]
    // 分母
    val denominatorStr = matchResult.groupValues[2]
    val numerator = numeratorStr.toDoubleOrNull() ?: throw ExcelException("无法解析分子: $numeratorStr，错误单元格值: ${excelIndex()}, 单元格坐标: $exCell ")
    val denominator = denominatorStr.toDoubleOrNull() ?: throw ExcelException("无法解析分母: $denominatorStr，错误单元格值: ${excelIndex()}, 单元格坐标: $exCell ")
    if (denominator == 0.0) { throw ExcelException("分母不能为零，错误单元格值: ${excelIndex()}, 单元格坐标: $exCell ") }
    return numerator / denominator
}
/**
 * 获取单元格的 Excel 索引（如 A1, B2 等）
 * 将列索引转换为字母表示
 */
fun Cell.excelIndex(): String {
    val rowNum = rowIndex + 1
    val columnIndex = columnIndex

    // 将列索引转换为 Excel 列字母
    val columnLetter = buildString {
        var index = columnIndex
        while (index >= 0) {
            insert(0, 'A' + (index % 26))
            index = index / 26 - 1
        }
    }

    return "$columnLetter$rowNum"
}