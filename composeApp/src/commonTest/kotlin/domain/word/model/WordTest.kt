package domain.word.model

import utils.Language
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WordTest {

    @Test
    fun `identical words are considered same content`() {
        val first = createWord(original = "Hello", translation = "Hola")
        val second = createWord(original = "Hello", translation = "Hola")

        assertTrue(first.isSameContent(second))
    }

    @Test
    fun `comparison ignores case and trims whitespace`() {
        val first = createWord(original = "  Hello ", translation = " Hola ")
        val second = createWord(original = "hello", translation = "hola")

        assertTrue(first.isSameContent(second))
    }

    @Test
    fun `different translation is not same content`() {
        val first = createWord(original = "Hello", translation = "Hola")
        val second = createWord(original = "Hello", translation = "Salut")

        assertFalse(first.isSameContent(second))
    }

    @Test
    fun `different original word is not same content`() {
        val first = createWord(original = "Hello", translation = "Hola")
        val second = createWord(original = "Hi", translation = "Hola")

        assertFalse(first.isSameContent(second))
    }

    @Test
    fun `accented characters are treated case insensitively`() {
        val first = createWord(original = "Café", translation = "Crème")
        val second = createWord(original = "café", translation = "crème")

        assertTrue(first.isSameContent(second))
    }

    private fun createWord(
        id: Int = 1,
        original: String,
        translation: String
    ) = Word(
        id = id,
        originalWord = original,
        translation = translation,
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.SPANISH,
        level = 0,
        easeFactor = 2.5f,
        interval = 0,
        repetitions = 0,
        lastReviewDate = 0L,
        nextReviewDate = 0L
    )
}

