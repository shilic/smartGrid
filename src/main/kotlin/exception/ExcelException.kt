package exception

class ExcelException(
    override val message : String,
) : RuntimeException(message) {
}