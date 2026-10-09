package domain.word.model

import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class WordTest {

    @Test
    fun `identical words are the same word`() {
        val first = createWord(original = "Hello", translation = "Hola")
        val second = createWord(original = "Hello", translation = "Hola")

        assertEquals(first.identity, second.identity)
    }

    @Test
    fun `comparison ignores case and trims whitespace`() {
        val first = createWord(original = "  Hello ", translation = " Hola ")
        val second = createWord(original = "hello", translation = "hola")

        assertEquals(first.identity, second.identity)
    }

    @Test
    fun `different translation is a different word`() {
        val first = createWord(original = "Hello", translation = "Hola")
        val second = createWord(original = "Hello", translation = "Salut")

        assertNotEquals(first.identity, second.identity)
    }

    @Test
    fun `different original word is a different word`() {
        val first = createWord(original = "Hello", translation = "Hola")
        val second = createWord(original = "Hi", translation = "Hola")

        assertNotEquals(first.identity, second.identity)
    }

    @Test
    fun `accented characters are treated case insensitively`() {
        val first = createWord(original = "Café", translation = "Crème")
        val second = createWord(original = "café", translation = "crème")

        assertEquals(first.identity, second.identity)
    }

    @Test
    fun `the same word in another learning language is a different word`() {
        val first = createWord(original = "chat", translation = "cat", learning = Language.FRENCH)
        val second = createWord(original = "chat", translation = "cat", learning = Language.GERMAN)

        assertNotEquals(first.identity, second.identity)
    }

    private fun createWord(
        id: Int = 1,
        original: String,
        translation: String,
        learning: Language = Language.SPANISH,
    ) = Word(
        id = id,
        originalWord = original,
        translation = translation,
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = learning,
        level = 0,
        easeFactor = 2.5f,
        interval = 0,
        repetitions = 0,
        lastReviewDate = 0L,
        nextReviewDate = 0L
    )
}

