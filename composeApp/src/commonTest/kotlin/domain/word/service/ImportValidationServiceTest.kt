package domain.word.service

import domain.common.exceptionOrNull
import domain.common.getOrThrow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImportValidationServiceTest {

    private val service = ImportValidationService()

    @Test
    fun `single entry parses trimmed word translation and description`() = runTest {
        val result = service.validateAndParse("  hello  ,  hola  ,  greeting ")

        assertTrue(result.isSuccess)
        val words = result.getOrThrow()
        assertEquals(1, words.size)

        val word = words.first()
        assertEquals("hello", word.originalWord)
        assertEquals("hola", word.translation)
        assertEquals("greeting", word.description)
    }

    @Test
    fun `multiple entries separated by semicolons and new lines parse successfully`() = runTest {
        val input = """
            hello,hola
            goodbye,adiós;thanks,gracias,Thank you note
        """.trimIndent()

        val result = service.validateAndParse(input)

        assertTrue(result.isSuccess)
        val words = result.getOrThrow()
        assertEquals(3, words.size)
        assertEquals("hello", words[0].originalWord)
        assertEquals("goodbye", words[1].originalWord)
        assertEquals("thanks", words[2].originalWord)
        assertEquals("Thank you note", words[2].description)
    }

    @Test
    fun `entries with additional commas keep remaining text in description`() = runTest {
        val input = "Hello, my friend,Hola, mi amigo,Used as a friendly greeting"

        val result = service.validateAndParse(input)

        assertTrue(result.isSuccess)
        val words = result.getOrThrow()
        val word = words.first()
        assertEquals("Hello", word.originalWord)
        assertEquals("my friend", word.translation)
        assertEquals("Hola, mi amigo,Used as a friendly greeting", word.description)
    }

    @Test
    fun `comments and blank lines are ignored`() = runTest {
        val input = """
            # header line
            
            hello,hola
            # another comment
            ,invalid
            world,mundo
        """.trimIndent()

        val result = service.validateAndParse(input)

        assertTrue(result.isSuccess)
        val words = result.getOrThrow()
        assertEquals(2, words.size)
        assertTrue(words.none { it.originalWord.startsWith("#") })
    }

    @Test
    fun `empty input returns error`() = runTest {
        val result = service.validateAndParse("   \n ")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("empty") == true)
    }

    @Test
    fun `invalid format returns descriptive error`() = runTest {
        val result = service.validateAndParse("invalid-line-without-comma")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("expected", ignoreCase = true) == true)
    }
}

