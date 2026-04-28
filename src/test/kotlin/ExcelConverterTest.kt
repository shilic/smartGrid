import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import utils.ExcelConverter.toBase26
import utils.ExcelConverter.toDecimalNumber
import utils.base26
import utils.exCell

/**
 * 单元测试示例
 */
class ExcelConverterTest {

    @Test
    fun `test toBase26 conversion`() {
        assertEquals("A", 1.base26)
        assertEquals("Z", 26.base26)
        assertEquals("AA", 27.base26)
        assertEquals("AB", 28.base26)
        assertEquals("AZ", 52.toBase26())
        assertEquals("BA", 53.toBase26())
    }

    @Test
    fun `test toDecimalNumber conversion`() {
        assertEquals(1, "A".toDecimalNumber())
        assertEquals(26, "Z".toDecimalNumber())
        assertEquals(27, "AA".toDecimalNumber())
        assertEquals(28, "AB".toDecimalNumber())
    }

    @Test
    fun `test coordinate conversion`() {
        assertEquals("A1", (0 to 0).exCell)
        assertEquals("Z3", (2 to 25).exCell)
        assertEquals("AA1", (0 to 26).exCell)

        assertEquals(0 to 0, "A1".exCell)
        assertEquals(2 to 25, "Z3".exCell)
        assertEquals(0 to 26, "AA1".exCell)
    }

    @Test
    fun `test invalid inputs`() {
        assertThrows<IllegalArgumentException> { 0.toBase26() }
        assertThrows<IllegalArgumentException> { (-1).toBase26() }
        assertThrows<IllegalArgumentException> { "".toDecimalNumber() }
        assertThrows<IllegalArgumentException> { "A0".exCell }
        assertThrows<IllegalArgumentException> { "1A".exCell }
    }
}