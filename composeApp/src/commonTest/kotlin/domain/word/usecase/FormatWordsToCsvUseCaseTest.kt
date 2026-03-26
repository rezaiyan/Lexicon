package domain.word.usecase

import domain.word.model.ParsedWord
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatWordsToCsvUseCaseTest {

    private val useCase = FormatWordsToCsvUseCase()

    @Test
    fun `formats single word without description`() {
        val result = useCase(listOf(ParsedWord("hello", "hola")))
        assertEquals("hello,hola", result)
    }

    @Test
    fun `formats single word with description`() {
        val result = useCase(listOf(ParsedWord("hello", "hola", "a greeting")))
        assertEquals("hello,hola,a greeting", result)
    }

    @Test
    fun `separates multiple words with newline`() {
        val result = useCase(listOf(
            ParsedWord("hello", "hola"),
            ParsedWord("goodbye", "adios"),
        ))
        assertEquals("hello,hola\ngoodbye,adios", result)
    }

    @Test
    fun `omits description field when blank`() {
        val result = useCase(listOf(
            ParsedWord("hello", "hola", ""),
            ParsedWord("bye", "adios", "  "),
        ))
        assertEquals("hello,hola\nbye,adios", result)
    }

    @Test
    fun `returns empty string for empty list`() {
        assertEquals("", useCase(emptyList()))
    }

    @Test
    fun `mixed words with and without description`() {
        val result = useCase(listOf(
            ParsedWord("hello", "hola", "greeting"),
            ParsedWord("bye", "adios"),
        ))
        assertEquals("hello,hola,greeting\nbye,adios", result)
    }
}
