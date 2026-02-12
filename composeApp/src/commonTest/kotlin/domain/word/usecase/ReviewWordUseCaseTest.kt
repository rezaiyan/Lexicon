package domain.word.usecase

import domain.settings.model.ReviewSettings
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.GetReviewSettingsUseCase
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReviewWordUseCaseTest {

    private val settingsRepository = FakeSettingsRepository()
    private val reviewSettingsUseCase = GetReviewSettingsUseCase(settingsRepository)
    private val wordRepository = FakeWordRepository()
    private val useCase = ReviewWordUseCase(wordRepository, reviewSettingsUseCase)

    @Test
    fun `forgot answer drops level by penalty and resets repetitions`() = runTest {
        settingsRepository.setSettings(successesToAdvance = 1, forgotPenalty = 2)
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
    fun `remembered answer advances level after threshold and resets repetitions`() = runTest {
        settingsRepository.setSettings(successesToAdvance = 2, forgotPenalty = 2)
        val word = createWord(level = 0, repetitions = 1, easeFactor = 2.2f, interval = 1)

        useCase(word, quality = 1)

        val updated = wordRepository.lastUpdatedWord
        assertNotNull(updated)
        assertEquals(1, updated.level)
        assertEquals(0, updated.repetitions)
        assertEquals(10, updated.interval)
        assertEquals(2.3f, updated.easeFactor)
        assertTrue(updated.nextReviewDate > word.nextReviewDate)
    }

    @Test
    fun `remembered answer without reaching threshold keeps level and repetitions`() = runTest {
        settingsRepository.setSettings(successesToAdvance = 3, forgotPenalty = 2)
        val word = createWord(level = 2, repetitions = 1, easeFactor = 2.0f, interval = 1)

        useCase(word, quality = 1)

        val updated = wordRepository.lastUpdatedWord
        assertNotNull(updated)
        assertEquals(2, updated.level)
        assertEquals(2, updated.repetitions)
        assertEquals(1, updated.interval) // level 2 interval (1 day)
        assertEquals(2.0f, updated.easeFactor)
    }

    @Test
    fun `mastered word grows interval exponentially on success`() = runTest {
        settingsRepository.setSettings(successesToAdvance = 1, forgotPenalty = 2)
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
        settingsRepository.setSettings(successesToAdvance = 1, forgotPenalty = 3)
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
        override suspend fun deleteAllWords(): Result<Unit> = Result.success(Unit)
        override suspend fun syncWithRemote(): Result<Unit> = Result.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun getTotalCount(): Int = 0
        override suspend fun getDueCount(): Int = 0

        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf(DeleteWordsProgress.Completed(0))
        override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
    }

    private class FakeSettingsRepository : ISettingsRepository {
        private val successesFlow = MutableStateFlow(ReviewSettings.BALANCED.successesToAdvance)
        private val penaltyFlow = MutableStateFlow(ReviewSettings.BALANCED.forgotPenalty)

        fun setSettings(successesToAdvance: Int, forgotPenalty: Int) {
            successesFlow.value = successesToAdvance
            penaltyFlow.value = forgotPenalty
        }

        override fun getSuccessesToAdvance(): Flow<Int> = successesFlow
        override fun getForgotPenalty(): Flow<Int> = penaltyFlow

        override suspend fun setSuccessesToAdvance(count: Int) {
            successesFlow.value = count
        }

        override suspend fun setForgotPenalty(levels: Int) {
            penaltyFlow.value = levels
        }

        override fun getLanguage(): Flow<utils.Language> = flowOf(utils.Language.ENGLISH)
        override suspend fun setLanguage(language: utils.Language) {}
        override fun getThemeMode(): Flow<domain.settings.model.ThemeMode> = flowOf(domain.settings.model.ThemeMode.AUTO)
        override suspend fun setThemeMode(mode: domain.settings.model.ThemeMode) {}
        override suspend fun getLastInsightDate(): String? = null
        override suspend fun getCachedInsight(): String? = null
        override suspend fun updateDailyInsight(date: String, insight: String) {}
        override suspend fun getLastInsightDismissedTime(): Long = 0L
        override suspend fun setLastInsightDismissedTime(timestamp: Long) {}
        override suspend fun clearInsightData() {}
        override suspend fun clearSettings() {}
        override fun getNotificationsEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setNotificationsEnabled(enabled: Boolean) {}
        override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setReviewRemindersEnabled(enabled: Boolean) {}
        override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setMotivationalMessagesEnabled(enabled: Boolean) {}
        override suspend fun getDailyReminderTime(): String = "09:00"
        override suspend fun setDailyReminderTime(time: String) {}
        override suspend fun getMinimumDueCards(): Int = 0
        override suspend fun setMinimumDueCards(count: Int) {}
    }
}

