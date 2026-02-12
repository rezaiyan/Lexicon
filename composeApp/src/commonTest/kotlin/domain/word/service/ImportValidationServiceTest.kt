package domain.word.service

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ImportValidationServiceTest {

    private val service = ImportValidationService()

    @Test
    fun `single entry parses trimmed word translation and description`() = runTest {
        val result = service.validateAndParse("  hello  ,  hola  ,  greeting ")

        val success = assertIs<IImportValidationService.ValidationResult.Success>(result)
        assertEquals(1, success.words.size)

        val word = success.words.first()
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

        val success = assertIs<IImportValidationService.ValidationResult.Success>(result)
        assertEquals(3, success.words.size)
        assertEquals("hello", success.words[0].originalWord)
        assertEquals("goodbye", success.words[1].originalWord)
        assertEquals("thanks", success.words[2].originalWord)
        assertEquals("Thank you note", success.words[2].description)
    }

    @Test
    fun `entries with additional commas keep remaining text in description`() = runTest {
        val input = "Hello, my friend,Hola, mi amigo,Used as a friendly greeting"

        val result = service.validateAndParse(input)

        val success = assertIs<IImportValidationService.ValidationResult.Success>(result)
        val word = success.words.first()
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

        val success = assertIs<IImportValidationService.ValidationResult.Success>(result)
        assertEquals(2, success.words.size)
        assertTrue(success.words.none { it.originalWord.startsWith("#") })
    }

    @Test
    fun `empty input returns error`() = runTest {
        val result = service.validateAndParse("   \n ")

        val error = assertIs<IImportValidationService.ValidationResult.Error>(result)
        assertTrue(error.message.contains("empty"))
    }

    @Test
    fun `invalid format returns descriptive error`() = runTest {
        val result = service.validateAndParse("invalid-line-without-comma")

        val error = assertIs<IImportValidationService.ValidationResult.Error>(result)
        assertTrue(error.message.contains("expected", ignoreCase = true))
    }
}

