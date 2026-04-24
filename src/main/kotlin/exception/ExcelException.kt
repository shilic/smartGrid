package person.shilicheng.exception

class ExcelException(
    override val message : String,
) : RuntimeException(message) {
}