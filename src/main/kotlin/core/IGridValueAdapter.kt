package core

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Sheet

/** 表格值转换器。需要表格值类型枚举来实现。
 *
 * 提供两个方法: 1.从表格组件获取值，并解析到对象的属性上。 2. 从指定的对象属性上获取值，并反馈到表格中。
 * */
interface IGridValueAdapter {
    /** 适配器的名称 */
    val adapterName: String
    fun parseGridCell(gridParser:IGridParser, cell: Cell?, sheet: Sheet, bind : GridColumnInfo, rowIndex: Ref<Int>, father: IGridRowData) : Any
}