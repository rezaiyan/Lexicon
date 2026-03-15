package domain.word.usecase

import core.common.Try
import domain.settings.usecase.GetReviewSettingsUseCase
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// GetReviewSettingsUseCase always returns ReviewSettings.BALANCED:
//   successesToAdvance = 1, forgotPenalty = 2
class ReviewWordUseCaseTest {

    private val reviewSettingsUseCase = GetReviewSettingsUseCase()
    private val wordRepository = FakeWordRepository()
    private val useCase = ReviewWordUseCase(wordRepository, reviewSettingsUseCase)

    @Test
    fun `forgot answer drops level by penalty and resets repetitions`() = runTest {
        // BALANCED forgotPenalty = 2 → level 3 - 2 = level 1
        val word = createWord(level = 3, repetitions = 4, easeFactor = 2.5f, interval = 7)

        useCase(word, quality = 0)

        val updated = wordRepository.lastUpdatedWord
        assertNotNull(updated)
        assertEquals(1, updated.level)
        assertEquals(0, updated.repetitions)
        assertEquals(10, updated.interval) // level 1 interval (10 minutes)
        assertEquals(2.3f, updated.easeFactor)
        assertTrue(updated.nextReviewDate > word.nextReviewDate)
        assertEquals(1, wordRepository.updateCount)
    }

    @Test
    fun `remembered answer advances level and resets repetitions`() = runTest {
        // BALANCED successesToAdvance = 1 → every success advances the level
        val word = createWord(level = 0, repetitions = 0, easeFactor = 2.2f, interval = 1)

        useCase(word, quality = 1)

        val updated = wordRepository.lastUpdatedWord
        assertNotNull(updated)
        assertEquals(1, updated.level)
        assertEquals(0, updated.repetitions)
        assertEquals(10, updated.interval) // level 1 interval (10 minutes)
        assertEquals(2.3f, updated.easeFactor)
        assertTrue(updated.nextReviewDate > word.nextReviewDate)
    }

    @Test
    fun `forgot answer at level 0 stays at level 0`() = runTest {
        // Tests floor boundary: level cannot drop below 0
        val word = createWord(level = 0, repetitions = 3, easeFactor = 2.0f, interval = 1)

        useCase(word, quality = 0)

        val updated = wordRepository.lastUpdatedWord
        assertNotNull(updated)
        assertEquals(0, updated.level)
        assertEquals(0, updated.repetitions)
        assertEquals(1, updated.interval) // level 0 interval (1 minute)
        assertEquals(1.8f, updated.easeFactor)
    }

    @Test
    fun `mastered word grows interval exponentially on success`() = runTest {
        val word = createWord(level = 6, repetitions = 2, easeFactor = 2.5f, interval = 30)

        useCase(word, quality = 1)

        val updated = wordRepository.lastUpdatedWord
        assertNotNull(updated)
        assertEquals(6, updated.level)
        assertEquals(3, updated.repetitions) // incremented
        assertEquals(75, updated.interval) // 30 * 2.5
        assertEquals(2.5f, updated.easeFactor) // unchanged at max
    }

    @Test
    fun `level 5 advances to level 6 on success`() = runTest {
        val word = createWord(level = 5, repetitions = 0, easeFactor = 2.0f, interval = 14)

        useCase(word, quality = 1)

        val updated = wordRepository.lastUpdatedWord
        assertNotNull(updated)
        assertEquals(6, updated.level)
        assertEquals(0, updated.repetitions) // Reset for new level
        assertEquals(30, updated.interval) // Level 6 base interval
        assertEquals(2.1f, updated.easeFactor) // +0.1
    }

    @Test
    fun `level 6 cannot advance beyond 6`() = runTest {
        val word = createWord(level = 6, repetitions = 0, easeFactor = 2.5f, interval = 30)

        useCase(word, quality = 1)

        val updated = wordRepository.lastUpdatedWord
        assertNotNull(updated)
        assertEquals(6, updated.level) // Stays at 6
    }

    @Test
    fun `mastered interval capped at 365 days`() = runTest {
        // With interval=200 and easeFactor=2.5, next would be 500 → capped at 365
        val word = createWord(level = 6, repetitions = 2, easeFactor = 2.5f, interval = 200)

        useCase(word, quality = 1)

        val updated = wordRepository.lastUpdatedWord
        assertNotNull(updated)
        assertEquals(365, updated.interval) // Capped at 1 year
    }

    @Test
    fun `ease factor never drops below 1_3`() = runTest {
        val word = createWord(level = 2, repetitions = 0, easeFactor = 1.3f, interval = 1)

        useCase(word, quality = 0)

        val updated = wordRepository.lastUpdatedWord
        assertNotNull(updated)
        assertEquals(1.3f, updated.easeFactor) // Floor at 1.3
    }

    @Test
    fun `ease factor never exceeds 2_5`() = runTest {
        val word = createWord(level = 3, repetitions = 0, easeFactor = 2.5f, interval = 3)

        useCase(word, quality = 1)

        val updated = wordRepository.lastUpdatedWord
        assertNotNull(updated)
        assertEquals(2.5f, updated.easeFactor) // Capped at 2.5
    }

    @Test
    fun `level 0 and 1 use minutes for next review - level 2 plus uses days`() = runTest {
        // Level 0 → level 1 (10 minutes in millis)
        val word0 = createWord(level = 0, repetitions = 0, easeFactor = 2.5f, interval = 1)
        useCase(word0, quality = 1)
        val updated0 = wordRepository.lastUpdatedWord
        assertNotNull(updated0)
        assertEquals(1, updated0.level)
        // Level 1 interval = 10 minutes → next review is ~10 min from now
        val tenMinutesInMillis = 10 * 60 * 1000L
        val range0 = (tenMinutesInMillis - 1000)..(tenMinutesInMillis + 1000)
        assertTrue(updated0.nextReviewDate - updated0.lastReviewDate in range0)

        // Level 1 → level 2 (1 day in millis)
        val word1 = createWord(level = 1, repetitions = 0, easeFactor = 2.5f, interval = 10)
        useCase(word1, quality = 1)
        val updated1 = wordRepository.lastUpdatedWord
        assertNotNull(updated1)
        assertEquals(2, updated1.level)
        val oneDayInMillis = 24 * 60 * 60 * 1000L
        val range1 = (oneDayInMillis - 1000)..(oneDayInMillis + 1000)
        assertTrue(updated1.nextReviewDate - updated1.lastReviewDate in range1)
    }

    @Test
    fun `forgot at level 1 drops to level 0 with penalty 2`() = runTest {
        // BALANCED forgotPenalty = 2, level 1 - 2 = -1 → clamped to 0
        val word = createWord(level = 1, repetitions = 3, easeFactor = 2.0f, interval = 10)

        useCase(word, quality = 0)

        val updated = wordRepository.lastUpdatedWord
        assertNotNull(updated)
        assertEquals(0, updated.level)
        assertEquals(1, updated.interval) // Level 0 interval
    }

    @Test
    fun `invalid quality is treated as forgot with configured penalty`() = runTest {
        // BALANCED forgotPenalty = 2 → level 2 - 2 = level 0
        val word = createWord(level = 2, repetitions = 5, easeFactor = 1.5f, interval = 3)

        useCase(word, quality = 5)

        val updated = wordRepository.lastUpdatedWord
        assertNotNull(updated)
        assertEquals(0, updated.level)
        assertEquals(0, updated.repetitions)
        assertEquals(1, updated.interval)
        assertEquals(1.3f, updated.easeFactor) // cannot drop below 1.3f
    }

    private fun createWord(
        id: Int = 1,
        level: Int,
        repetitions: Int,
        easeFactor: Float,
        interval: Int,
        lastReviewDate: Long = 0L,
        nextReviewDate: Long = 0L
    ) = Word(
        id = id,
        originalWord = "hello",
        translation = "hola",
        description = "greeting",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.SPANISH,
        level = level,
        easeFactor = easeFactor,
        interval = interval,
        repetitions = repetitions,
        lastReviewDate = lastReviewDate,
        nextReviewDate = nextReviewDate
    )

    private class FakeWordRepository : IWordRepository {
        var lastUpdatedWord: Word? = null
        var updateCount: Int = 0

        override suspend fun updateWord(word: Word): Try<Unit> {
            lastUpdatedWord = word
            updateCount++
            return Try.success(Unit)
        }

        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
        override suspend fun insertWords(words: List<Word>): Try<Int> = Try.success(words.size)
        override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override suspend fun getTotalCount(): Try<Int> = Try.success(0)
        override suspend fun getDueCount(): Try<Int> = Try.success(0)

        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf(DeleteWordsProgress.Completed(0))
        override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
        override fun updateWordsLanguages(
            ids: List<Int>,
            sourceLanguage: String,
            targetLanguage: String,
        ): Flow<UpdateWordsLanguagesProgress> =
            flow { emit(UpdateWordsLanguagesProgress.Completed(ids.size)) }
        override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
    }
}
