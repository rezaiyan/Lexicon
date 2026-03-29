package feature.study.wordrush

import core.common.fold
import domain.word.model.Word
import domain.word.usecase.GetWordRushWordsUseCase
import fakes.FakeWordRepository
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetWordRushWordsUseCaseTest {

    private fun createWords(count: Int): List<Word> = (1..count).map { i ->
        Word(
            id = i,
            originalWord = "word_$i",
            translation = "translation_$i",
            description = "desc_$i",
            sourceLanguage = Language.ENGLISH,
            targetLanguage = Language.GERMAN,
            nextReviewDate = 0L,
        )
    }

    private fun fakeRepo(words: List<Word>): FakeWordRepository {
        return FakeWordRepository().apply {
            storedWords = words.toMutableList()
        }
    }

    @Test
    fun `returns requested number of words when enough exist`() = runTest {
        val useCase = GetWordRushWordsUseCase(fakeRepo(createWords(10)))
        val result = useCase(5)
        assertTrue(result.isSuccess)
        result.fold(
            onSuccess = { assertEquals(5, it.size) },
            onFailure = { throw it },
        )
    }

    @Test
    fun `fails when fewer than 4 words exist`() = runTest {
        val useCase = GetWordRushWordsUseCase(fakeRepo(createWords(3)))
        val result = useCase(3)
        assertTrue(result.isFailure)
    }

    @Test
    fun `returns all words when requested count exceeds available`() = runTest {
        val useCase = GetWordRushWordsUseCase(fakeRepo(createWords(6)))
        val result = useCase(10)
        assertTrue(result.isSuccess)
        result.fold(
            onSuccess = { assertEquals(6, it.size) },
            onFailure = { throw it },
        )
    }

    @Test
    fun `exactly 4 words succeeds`() = runTest {
        val useCase = GetWordRushWordsUseCase(fakeRepo(createWords(4)))
        val result = useCase(4)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `succeeds when 4 words span different learning stages`() = runTest {
        // Regression: words at non-zero levels must NOT be excluded from Word Rush.
        // A user with words at FRESH(0), FAMILIAR(2), ALMOST(4), MASTERED(6) should be able to play.
        val words = listOf(
            Word(id = 1, originalWord = "word_1", translation = "translation_1", description = "desc_1", sourceLanguage = Language.ENGLISH, targetLanguage = Language.GERMAN, level = 0, nextReviewDate = 0L),
            Word(id = 2, originalWord = "word_2", translation = "translation_2", description = "desc_2", sourceLanguage = Language.ENGLISH, targetLanguage = Language.GERMAN, level = 2, nextReviewDate = 0L),
            Word(id = 3, originalWord = "word_3", translation = "translation_3", description = "desc_3", sourceLanguage = Language.ENGLISH, targetLanguage = Language.GERMAN, level = 4, nextReviewDate = 0L),
            Word(id = 4, originalWord = "word_4", translation = "translation_4", description = "desc_4", sourceLanguage = Language.ENGLISH, targetLanguage = Language.GERMAN, level = 6, nextReviewDate = 0L),
        )
        val useCase = GetWordRushWordsUseCase(fakeRepo(words))
        val result = useCase(GetWordRushWordsUseCase.MINIMUM_WORDS)
        assertTrue(result.isSuccess)
        result.fold(
            onSuccess = { assertEquals(GetWordRushWordsUseCase.MINIMUM_WORDS, it.size) },
            onFailure = { throw it },
        )
    }
}
