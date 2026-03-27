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
}
