package domain.word.usecase

import domain.common.Try
import domain.settings.usecase.GetReviewSettingsUseCase
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
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
        sourceLanguage = "en",
        targetLanguage = "es",
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

        override suspend fun updateWord(word: Word) {
            lastUpdatedWord = word
            updateCount++
        }

        override suspend fun getAllWordsAsync(): List<Word> = emptyList()
        override suspend fun insertWords(words: List<Word>): Int = words.size
        override suspend fun deleteWord(id: Int) {}
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override suspend fun getTotalCount(): Int = 0
        override suspend fun getDueCount(): Int = 0

        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf(DeleteWordsProgress.Completed(0))
        override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
    }
}
