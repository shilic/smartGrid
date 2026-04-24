package person.shilicheng.utils

import org.apache.poi.ss.usermodel.Cell
import kotlin.math.pow

/**
 * Excel 转换器，将数值类型转换为 26 进制字符串;
 * 例如 A 表示 1，Z 表示 26，AA 表示 27，AB 表示 28;
 * 使用 Kotlin 的函数式编程、扩展函数和现代语法优化;
 */
object ExcelConverter {
    /**
     * 将十进制数字转换为 26 进制字母的列表;
     * 例如 28 转换为字符数组 = {A, B};
     * 1 变成 A，26 变 Z，27 = AA，28 AB;
     *
     * @param decimalNumber 十进制数，必须大于 0
     * @return A 到 Z 的列表，A 表示 1，Z 表示 26，十位的 A 表示一个 26
     */
    fun Int.toBase26List(): List<Char> {
        require(this > 0) { "转换Excel坐标错误, 错误值:${this@toBase26List}, 十进制数必须大于 0" }

        val result = mutableListOf<Char>()
        var number = this

        while (number > 0) {
            val remainder = (number - 1) % 26
            val letter = ('A'.code + remainder).toChar()
            // 插入到列表开头
            result.add(0, letter)
            number = (number - 1) / 26
        }
        return result
    }

    /**
     * 将十进制数字转换为 26 进制字母字符串
     * 例如 28 转换为 "AB"
     *
     * @param decimalNumber 十进制数，必须大于 0
     * @return 26 进制字符串
     */
    fun Int.toBase26(): String = toBase26List().joinToString("")

    /**
     * 将 26 进制字母列表转换为十进制数
     * 例如 [A, B] 转换为 28
     * TODO
     * @param twentySixBase 26 进制字母列表
     * @return 十进制数
     */
    fun List<Char>.toDecimalNumber(): Int {
        require(this.isNotEmpty()) { "转换Excel坐标错误, 错误值:${this@toDecimalNumber.joinToString()}, 26 进制字母列表不能为空" }
        require(this.all { it in 'A'..'Z' }) { "只能包含 A-Z 的字母" }
        return reversed()  // 从低位到高位处理
            .mapIndexed { index, char ->
                // 计算十进制的值，例如，字符A就得1 。
                val value = (char - 'A' + 1)
                // 使用幂运算计算每一位的权重
                value * 26.0.pow(index.toDouble()).toInt()
            }
            .sum()
    }

    /**
     * 将 26 进制字符串转换为十进制数;
     * 例如 "AB" 转换为 28, AA=27;Z=26;A=1;
     *
     * @param str 26 进制字符串
     * @return 十进制数
     */
    fun String.toDecimalNumber(): Int = this.toCharArray().toList().toDecimalNumber()

    /**
     * 为单元格的行列索引创建 Excel 坐标字符串;
     * 例如 (2, 25) -> "Z3" 表示第 26 列第 3 行;
     * AA1表示第27列第一行。
     * rowIndex 行下标，从0开始计算;
     * columnIndex 列下标，从0开始计算;
     *
     * @param rowIndex 行索引，从 0 开始
     * @param columnIndex 列索引，从 0 开始
     * @return Excel 坐标字符串
     */
    fun Pair<Int, Int>.excelIndex(): String {
        val (rowIndex, columnIndex) = this
        require(rowIndex >= 0 && columnIndex >= 0) { "转换Excel坐标错误, 错误值:${this@excelIndex}, 行索引和列索引必须是非负数" }

        val columnStr = (columnIndex + 1).toBase26()
        val rowNum = rowIndex + 1
        return "$columnStr$rowNum"
    }

    /**
     * 获取单元格的 Excel 坐标字符串
     * 例如 Z3 表示第 26 列第 3 行, AA1表示第27列第一行。
     *
     * @receiver 单元格对象
     * @return Excel 坐标字符串
     */
    fun Cell.excelIndex(): String {
        val columnStr = (columnIndex + 1).toBase26()
        val rowNum = rowIndex + 1
        return "$columnStr$rowNum"
    }

    /**
     * 将 Excel 坐标字符串转换为行列索引;
     * 例如 "Z3" -> (2, 25) 表示第 3 行第 26 列;
     * 返回一个(int rowIndex, int columnIndex)元组：
     * rowIndex 行下标，从0开始计算;
     * columnIndex 列下标，从0开始计算;
     *
     * @param excelCoordinate Excel 坐标字符串
     * @return 行索引和列索引的 Pair，从 0 开始
     */
    fun String.excelIndex(): Pair<Int, Int> {
        val pattern = Regex("^(?<column>[A-Z]+)(?<row>\\d+)$")

        val matchResult = pattern.matchEntire(this) ?: throw IllegalArgumentException("转换Excel坐标错误, 错误值:${this@excelIndex}, 无效的 Excel 坐标格式: $this")

        val columnStr = matchResult.groups["column"]?.value ?: throw IllegalArgumentException("转换Excel坐标错误, 错误值:${this@excelIndex}, 无法解析列部分: $this")
        val rowStr = matchResult.groups["row"]?.value ?: throw IllegalArgumentException("转换Excel坐标错误, 错误值:${this@excelIndex}, 无法解析行部分: $this")

        val columnIndex = columnStr.toDecimalNumber() - 1
        val rowIndex = rowStr.toInt() - 1

        require(rowIndex >= 0) { "行号必须大于等于 0" }
        require(columnIndex >= 0) { "列号必须大于等于 0" }

        return rowIndex to columnIndex
    }
}

/**
 * 将十进制数字转换为 26 进制字母字符串
 * 例如 28 转换为 "AB"
 *
 * @param decimalNumber 十进制数，必须大于 0
 * @return 26 进制字符串
 */
val Int.base26: String
    get() = ExcelConverter.run { this@base26.toBase26() }
/**
 * 将十进制数字转换为 26 进制字母的列表;
 * 例如 28 转换为字符数组 = {A, B};
 * 1 变成 A，26 变 Z，27 = AA，28 AB;
 *
 * @param decimalNumber 十进制数，必须大于 0
 * @return A 到 Z 的列表，A 表示 1，Z 表示 26，十位的 A 表示一个 26
 */
val Int.base26s: List<Char>
    get() = ExcelConverter.run { this@base26s.toBase26List() }

/**
 * 将 Excel 坐标字符串转换为行列索引;
 * 例如 "Z3" -> (2, 25) 表示第 3 行第 26 列;
 * 返回一个(int rowIndex, int columnIndex)元组：
 * rowIndex 行下标，从0开始计算;
 * columnIndex 列下标，从0开始计算;
 *
 * @param excelCoordinate Excel 坐标字符串
 * @return 行索引和列索引的 Pair，从 0 开始
 */
val String.exCell: Pair<Int, Int>
    get() = ExcelConverter.run { this@exCell.excelIndex() }

/**
 * 获取单元格的 Excel 坐标字符串
 * 例如 Z3 表示第 26 列第 3 行, AA1表示第27列第一行。
 *
 * @receiver 单元格对象
 * @return Excel 坐标字符串
 */
val Cell.exCell: String
    get() = ExcelConverter.run { this@exCell.excelIndex() }

/**
 * 为行列对添加扩展属性
 */
val Pair<Int, Int>.exCell: String
    get() = ExcelConverter.run { this@exCell.excelIndex() }


