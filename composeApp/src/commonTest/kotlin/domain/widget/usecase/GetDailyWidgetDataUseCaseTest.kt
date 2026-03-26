package domain.widget.usecase

import core.common.exceptionOrNull
import core.common.getOrNull
import domain.word.model.Word
import fakes.FakeWidgetRefresher
import fakes.FakeWordRepository
import fakes.fakeGetDailyWidgetDataUseCase
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GetDailyWidgetDataUseCaseTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun testWord(id: Int, original: String = "word$id", translation: String = "trans$id") = Word(
        id = id,
        originalWord = original,
        translation = translation,
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.ENGLISH,
        nextReviewDate = 0L,
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `fails with NoWordsAvailableException when word list is empty`() = runTest {
        val repo = FakeWordRepository() // storedWords is empty by default
        val useCase = fakeGetDailyWidgetDataUseCase(repo)

        val result = useCase(Unit)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertIs<NoWordsAvailableException>(exception)
    }

    @Test
    fun `returns DailyWidgetData with correct word on success`() = runTest {
        val word = testWord(1, "hello", "hola")
        val repo = FakeWordRepository().apply { storedWords.add(word) }
        val useCase = fakeGetDailyWidgetDataUseCase(repo)

        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        val data = result.getOrNull()!!
        assertEquals(word.id, data.wordId)
        assertEquals(word.originalWord, data.word)
        assertEquals(word.translation, data.translation)
    }

    @Test
    fun `pushes data to widget refresher after successful retrieval`() = runTest {
        val word = testWord(2, "apple", "manzana")
        val repo = FakeWordRepository().apply { storedWords.add(word) }
        val refresher = FakeWidgetRefresher()
        val useCase = GetDailyWidgetDataUseCase(repo, noOpStreakRepo(), refresher)

        useCase(Unit)

        assertNotNull(refresher.pushedData)
        assertEquals(word.id, refresher.pushedData!!.wordId)
    }

    @Test
    fun `same word is selected on same day - deterministic selection`() = runTest {
        val words = (1..10).map { testWord(it) }
        val repo = FakeWordRepository().apply { storedWords.addAll(words) }
        val useCase = fakeGetDailyWidgetDataUseCase(repo)

        val result1 = useCase(Unit)
        val result2 = useCase(Unit)

        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertEquals(result1.getOrNull()!!.wordId, result2.getOrNull()!!.wordId)
    }

    @Test
    fun `widget data includes correct dueCardCount`() = runTest {
        val word = testWord(3)
        val repo = FakeWordRepository().apply {
            storedWords.add(word)
            dueCountResult = core.common.Try.success(5)
        }
        val useCase = fakeGetDailyWidgetDataUseCase(repo)

        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull()!!.dueCardCount)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun noOpStreakRepo() = object : domain.streak.repository.IStreakRepository {
        override suspend fun getStreak(): core.common.Try<domain.streak.model.StreakData> =
            core.common.Try.success(domain.streak.model.StreakData(0))
        override suspend fun recordActivity(count: Int): core.common.Try<domain.streak.model.StreakData> =
            core.common.Try.success(domain.streak.model.StreakData(0))
    }
}
