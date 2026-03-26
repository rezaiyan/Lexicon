package domain.study

import domain.study.usecase.ResolveCardLanguageUseCase
import domain.word.model.Word
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveCardLanguageUseCaseTest {

    private val useCase = ResolveCardLanguageUseCase()

    private fun word(id: Int, original: String, src: Language = Language.ENGLISH, target: Language = Language.SPANISH) =
        Word(
            id = id, originalWord = original, translation = "t$id", description = "",
            sourceLanguage = src, targetLanguage = target,
            nextReviewDate = 0L,
        )

    @Test
    fun `returns explicit code when not blank`() {
        val result = useCase("hello", "fr", listOf(word(1, "hello")))
        assertEquals("fr", result)
    }

    @Test
    fun `returns explicit code when words list is empty`() {
        val result = useCase("hello", "de", emptyList())
        assertEquals("de", result)
    }

    @Test
    fun `returns fallback when explicit code blank and words empty`() {
        val result = useCase("hello", "", emptyList())
        assertEquals("", result)
    }

    @Test
    fun `resolves source language when text matches original word`() {
        val words = listOf(
            word(1, "hello", src = Language.ENGLISH, target = Language.SPANISH),
            word(2, "world", src = Language.ENGLISH, target = Language.SPANISH),
        )
        // "hello" appears as originalWord → source side → returns source language code
        val result = useCase("hello", "", words)
        assertEquals("en", result)
    }

    @Test
    fun `resolves target language when text does not match any original word`() {
        val words = listOf(
            word(1, "hello", src = Language.ENGLISH, target = Language.SPANISH),
            word(2, "world", src = Language.ENGLISH, target = Language.SPANISH),
        )
        // "hola" doesn't match any originalWord → target side → returns target language code
        val result = useCase("hola", "", words)
        assertEquals("es", result)
    }

    @Test
    fun `uses majority vote when languages are mixed on target side`() {
        val words = listOf(
            word(1, "a", src = Language.ENGLISH, target = Language.SPANISH),
            word(2, "b", src = Language.ENGLISH, target = Language.SPANISH),
            word(3, "c", src = Language.ENGLISH, target = Language.FRENCH),
        )
        // text "x" doesn't match any original → target side → majority is "es" (2 vs 1)
        val result = useCase("x", "", words)
        assertEquals("es", result)
    }
}
