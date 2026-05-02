package demo.dataModel.dbcDataModel

/** 报文矩阵中的分组类型 */
sealed class MatrixGroupType {
    abstract val value: String
    /** 分组标志位 */
    data object GroupFlag : MatrixGroupType() {
        override val value: String get() = "M"
    }
    /** 默认分组 */
    data object DefaultGroup : MatrixGroupType() {
        override val value: String get() = ""
    }
    /** 自定义组 */
    data class CustomGroup(val number: UInt) : MatrixGroupType() {
        override val value: String get() = "m$number"
    }
}