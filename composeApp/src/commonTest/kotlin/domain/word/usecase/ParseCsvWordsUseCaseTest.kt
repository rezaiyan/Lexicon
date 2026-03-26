package domain.word.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParseCsvWordsUseCaseTest {

    private val useCase = ParseCsvWordsUseCase()

    @Test
    fun `parses single word line`() {
        val result = useCase("hello,hola")
        assertEquals(1, result.size)
        assertEquals("hello", result[0].word)
        assertEquals("hola", result[0].translation)
        assertEquals("", result[0].description)
    }

    @Test
    fun `parses word with description`() {
        val result = useCase("hello,hola,a greeting")
        assertEquals(1, result.size)
        assertEquals("a greeting", result[0].description)
    }

    @Test
    fun `parses multiple lines separated by newline`() {
        val result = useCase("hello,hola\ngoodbye,adios")
        assertEquals(2, result.size)
    }

    @Test
    fun `parses multiple lines separated by semicolon`() {
        val result = useCase("hello,hola;goodbye,adios")
        assertEquals(2, result.size)
    }

    @Test
    fun `skips blank lines`() {
        val result = useCase("hello,hola\n\ngoodbye,adios")
        assertEquals(2, result.size)
    }

    @Test
    fun `skips comment lines`() {
        val result = useCase("# comment\nhello,hola")
        assertEquals(1, result.size)
    }

    @Test
    fun `skips lines with missing translation`() {
        val result = useCase("onlyword")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `skips lines with blank word`() {
        val result = useCase(",translation")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `trims whitespace from parts`() {
        val result = useCase(" hello , hola ")
        assertEquals("hello", result[0].word)
        assertEquals("hola", result[0].translation)
    }

    @Test
    fun `returns empty list for empty input`() {
        assertTrue(useCase("").isEmpty())
        assertTrue(useCase("   ").isEmpty())
    }

    @Test
    fun `description stops at third comma even if more commas present`() {
        val result = useCase("hello,hola,desc,extra")
        assertEquals("desc,extra", result[0].description)
    }
}
