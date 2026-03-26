package presentation.feature.study

import app.cash.turbine.test
import core.common.Try
import domain.analytics.model.ReviewEventParams
import domain.analytics.repository.IAnalyticsRecorder
import domain.analytics.usecase.EndStudySessionUseCase
import domain.analytics.usecase.RecordReviewEventUseCase
import domain.analytics.usecase.StartStudySessionUseCase
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.GetCurrentLanguageUseCase
import domain.settings.usecase.ObserveSpeechRateUseCase
import domain.settings.usecase.SetTtsSpeechRateUseCase
import domain.streak.model.StreakData
import domain.streak.repository.IStreakRepository
import domain.streak.usecase.RecordStreakActivityUseCase
import domain.tts.model.TtsModelInfo
import domain.tts.model.TtsState
import domain.tts.repository.ITtsRepository
import domain.tts.usecase.ObserveTtsStateUseCase
import domain.tts.usecase.SpeakWordUseCase
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.ReviewSource
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IReviewSyncRepository
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import domain.study.usecase.GenerateSessionIdUseCase
import domain.study.usecase.ResolveCardLanguageUseCase
import domain.word.usecase.DeleteWordUseCase
import domain.word.usecase.FlushReviewSyncQueueUseCase
import domain.word.usecase.GetDueWordsByTagUseCase
import domain.word.usecase.GetDueWordsUseCase
import domain.word.usecase.GetWordsByStageUseCase
import domain.word.usecase.LoadReviewQueueUseCase
import domain.word.usecase.ReviewWordUseCase
import domain.word.usecase.UpdateWordUseCase
import fakes.FakeAnalyticsTracker
import fakes.FakeWidgetRefresher
import fakes.fakeGetDailyWidgetDataUseCase
import feature.study.ReviewEffect
import feature.study.ReviewState
import feature.study.ReviewViewModel
import feature.study.model.ReviewError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReviewViewModelTest : ViewModelTestBase() {

    // ---------------------------------------------------------------------------
    // Shared test data
    // ---------------------------------------------------------------------------

    private fun testWord(id: Int) = Word(
        id = id,
        originalWord = "word$id",
        translation = "trans$id",
        description = "desc$id",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.GERMAN,
        nextReviewDate = 0L,
    )

    private var dueWords: List<Word> = listOf(testWord(1), testWord(2))
    private var stageWords: List<Word> = listOf(testWord(3))
    private var deleteResult: Try<Unit> = Try.success(Unit)
    private var updateResult: Try<Unit> = Try.success(Unit)
    private var throwOnLoad: Boolean = false

    // ---------------------------------------------------------------------------
    // Fakes
    // ---------------------------------------------------------------------------

    /**
     * A ReviewSyncRepository fake that tracks how many times dequeueAll is called.
     * FlushReviewSyncQueueUseCase calls dequeueAll once per flush invocation, so
     * dequeueAllCallCount is a reliable proxy for flush call count.
     */
    private class FakeReviewSyncRepository : IReviewSyncRepository {
        var enqueueCallCount = 0
        var dequeueAllCallCount = 0

        override suspend fun enqueue(wordId: Int): Try<Unit> {
            enqueueCallCount++
            return Try.success(Unit)
        }

        override suspend fun dequeueAll(): Try<List<Int>> {
            dequeueAllCallCount++
            return Try.success(emptyList())
        }
    }

    private inner class FakeWordRepo : IWordRepository {
        override fun getDueCards(): Flow<List<Word>> = if (throwOnLoad) {
            flow { throw RuntimeException("network connect failed") }
        } else {
            flowOf(dueWords)
        }
        override fun getDueCardsByTag(tagId: Long): Flow<List<Word>> = if (throwOnLoad) {
            flow { throw RuntimeException("network connect failed") }
        } else { flowOf(emptyList()) }
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = if (throwOnLoad) {
            flow { throw RuntimeException("network connect failed") }
        } else { flowOf(stageWords) }
        override suspend fun deleteWord(id: Int): Try<Unit> = deleteResult
        override suspend fun updateWord(word: Word): Try<Unit> = updateResult
        override suspend fun updateWordLocal(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun batchSyncWords(words: List<Word>): Try<Unit> = Try.success(Unit)
        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun insertWords(words: List<Word>): Try<Int> = Try.success(0)
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf()
        override fun updateWordsLanguages(
            ids: List<Int>,
            sourceLanguage: String,
            targetLanguage: String,
        ): Flow<UpdateWordsLanguagesProgress> = flowOf()
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = emptyFlow()
        override suspend fun getTotalCount(): Try<Int> = Try.success(0)
        override suspend fun getDueCount(): Try<Int> = Try.success(0)
        override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
    }

    private fun fakeSettingsRepo() = object : ISettingsRepository {
        override fun getLanguage(): Flow<Language> = flowOf(Language.ENGLISH)
        override suspend fun setLanguage(language: Language): Try<Unit> = Try.success(Unit)
        override fun getThemeMode(): Flow<ThemeMode> = flowOf(ThemeMode.AUTO)
        override suspend fun setThemeMode(mode: ThemeMode): Try<Unit> = Try.success(Unit)
        override suspend fun clearSettings(): Try<Unit> = Try.success(Unit)
        override fun getNotificationsEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setNotificationsEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setReviewRemindersEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setMotivationalMessagesEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override suspend fun getDailyReminderTime(): Try<String> = Try.success("09:00")
        override suspend fun setDailyReminderTime(time: String): Try<Unit> = Try.success(Unit)
        override suspend fun getMinimumDueCards(): Try<Int> = Try.success(5)
        override suspend fun setMinimumDueCards(count: Int): Try<Unit> = Try.success(Unit)
    }

    private fun fakeStreakRepo() = object : IStreakRepository {
        override suspend fun getStreak(): Try<StreakData> = Try.success(StreakData(0))
        override suspend fun recordActivity(count: Int): Try<StreakData> = Try.success(StreakData(1))
    }

    private fun fakeTtsRepo() = object : ITtsRepository {
        override val ttsState: MutableStateFlow<TtsState> = MutableStateFlow(TtsState.Idle)
        override suspend fun speak(text: String, languageCode: String): Try<Unit> = Try.success(Unit)
        override suspend fun stop(): Try<Unit> = Try.success(Unit)
        override suspend fun isModelDownloaded(languageCode: String): Try<Boolean> = Try.success(true)
        override suspend fun downloadModel(languageCode: String): Flow<Float> = flowOf(1f)
        override fun isLanguageSupported(languageCode: String): Boolean = true
        override fun getSupportedLanguageCodes(): Set<String> = setOf("en")
        override suspend fun getModelInfo(languageCode: String, displayName: String): Try<TtsModelInfo> =
            Try.success(TtsModelInfo(languageCode, displayName, false, 0L))
        override suspend fun deleteModel(languageCode: String): Try<Unit> = Try.success(Unit)
    }

    private fun fakeAnalyticsRecorder() = object : IAnalyticsRecorder {
        override suspend fun startSession(sessionId: String, reviewType: String, startedAt: Long): Try<Unit> =
            Try.success(Unit)
        override suspend fun endSession(
            sessionId: String, endedAt: Long, durationMs: Long,
            totalCards: Int, correctCount: Int, incorrectCount: Int, completedNormally: Boolean,
        ): Try<Unit> = Try.success(Unit)
        override suspend fun recordReviewEvent(params: ReviewEventParams): Try<Unit> = Try.success(Unit)
        override suspend fun retryPendingSync(): Try<Unit> = Try.success(Unit)
    }

    // ---------------------------------------------------------------------------
    // ViewModel factory
    // ---------------------------------------------------------------------------

    // Captured so individual tests can inspect flush call counts via dequeueAllCallCount.
    private lateinit var syncRepo: FakeReviewSyncRepository

    private fun createViewModel(): ReviewViewModel {
        val wordRepo = FakeWordRepo()
        syncRepo = FakeReviewSyncRepository()
        val settingsRepo = fakeSettingsRepo()
        val ttsRepo = fakeTtsRepo()
        val recorder = fakeAnalyticsRecorder()
        return ReviewViewModel(
            loadQueueUseCase = LoadReviewQueueUseCase(
                getDueWords = GetDueWordsUseCase(wordRepo),
                getWordsByStage = GetWordsByStageUseCase(wordRepo),
                getDueWordsByTag = GetDueWordsByTagUseCase(wordRepo),
            ),
            reviewWordUseCase = ReviewWordUseCase(wordRepo, syncRepo),
            flushReviewSyncQueueUseCase = FlushReviewSyncQueueUseCase(syncRepo, wordRepo),
            updateWordUseCase = UpdateWordUseCase(wordRepo),
            deleteWordUseCase = DeleteWordUseCase(
                wordRepo, FakeWidgetRefresher(), fakeGetDailyWidgetDataUseCase(wordRepo),
            ),
            startSessionUseCase = StartStudySessionUseCase(recorder),
            endSessionUseCase = EndStudySessionUseCase(recorder),
            recordEventUseCase = RecordReviewEventUseCase(recorder),
            recordStreakUseCase = RecordStreakActivityUseCase(fakeStreakRepo()),
            speakWordUseCase = SpeakWordUseCase(ttsRepo, GetCurrentLanguageUseCase(settingsRepo)),
            observeTtsState = ObserveTtsStateUseCase(ttsRepo),
            observeSpeechRate = ObserveSpeechRateUseCase(settingsRepo),
            setSpeechRateUseCase = SetTtsSpeechRateUseCase(settingsRepo),
            generateSessionIdUseCase = GenerateSessionIdUseCase(),
            resolveCardLanguageUseCase = ResolveCardLanguageUseCase(),
            analyticsTracker = FakeAnalyticsTracker(),
        )
    }

    // ---------------------------------------------------------------------------
    // Initial state
    // ---------------------------------------------------------------------------

    @Test
    fun `initial review state is Idle`() {
        val vm = createViewModel()
        assertIs<ReviewState.Idle>(vm.currentState.review)
    }

    @Test
    fun `initial ttsState is Idle`() {
        val vm = createViewModel()
        assertEquals(TtsState.Idle, vm.currentState.ttsState)
    }

    // ---------------------------------------------------------------------------
    // startSession
    // ---------------------------------------------------------------------------

    @Test
    fun `startSession DueCards transitions to Active with due words`() = runTest {
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        val state = vm.currentState.review
        assertIs<ReviewState.Active>(state)
        assertEquals(2, state.words.size)
        assertEquals(0, state.currentIndex)
    }

    @Test
    fun `startSession ByStage transitions to Active with stage words`() = runTest {
        val vm = createViewModel()
        vm.startSession(ReviewSource.ByStage(LearningStage.LEVEL_0_FRESH))
        val state = vm.currentState.review
        assertIs<ReviewState.Active>(state)
        assertEquals(1, state.words.size)
        assertEquals(testWord(3).id, state.words.first().id)
    }

    @Test
    fun `startSession with empty word list transitions to Empty`() = runTest {
        dueWords = emptyList()
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        assertIs<ReviewState.Empty>(vm.currentState.review)
    }

    @Test
    fun `startSession load failure transitions to Error`() = runTest {
        throwOnLoad = true
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        val state = vm.currentState.review
        assertIs<ReviewState.Error>(state)
        assertIs<ReviewError.Network>(state.error)
        assertEquals(ReviewSource.DueCards, state.source)
    }

    @Test
    fun `startSession error source is preserved for retry`() = runTest {
        throwOnLoad = true
        val vm = createViewModel()
        val source = ReviewSource.ByStage(LearningStage.LEVEL_1_LEARNING)
        vm.startSession(source)
        val state = vm.currentState.review as ReviewState.Error
        assertEquals(source, state.source)
    }

    @Test
    fun `startSession flushes the sync queue before loading words`() = runTest {
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        // dequeueAll is called exactly once per FlushReviewSyncQueueUseCase invocation
        assertEquals(1, syncRepo.dequeueAllCallCount)
    }

    @Test
    fun `startSession flushes queue even when no words are due`() = runTest {
        dueWords = emptyList()
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        assertEquals(1, syncRepo.dequeueAllCallCount)
        assertIs<ReviewState.Empty>(vm.currentState.review)
    }

    @Test
    fun `startSession flushes queue even when load fails`() = runTest {
        throwOnLoad = true
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        assertEquals(1, syncRepo.dequeueAllCallCount)
        assertIs<ReviewState.Error>(vm.currentState.review)
    }

    // ---------------------------------------------------------------------------
    // reviewWord
    // ---------------------------------------------------------------------------

    @Test
    fun `reviewWord correct increments knownCount and advances index`() = runTest {
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        vm.reviewWord(quality = 1)
        val state = vm.currentState.review
        assertIs<ReviewState.Active>(state)
        assertEquals(1, state.knownCount)
        assertEquals(0, state.unknownCount)
        assertEquals(1, state.currentIndex)
    }

    @Test
    fun `reviewWord incorrect increments unknownCount and advances index`() = runTest {
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        vm.reviewWord(quality = 0)
        val state = vm.currentState.review
        assertIs<ReviewState.Active>(state)
        assertEquals(0, state.knownCount)
        assertEquals(1, state.unknownCount)
        assertEquals(1, state.currentIndex)
    }

    @Test
    fun `reviewWord on last card transitions to Completed`() = runTest {
        dueWords = listOf(testWord(1))
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        vm.reviewWord(quality = 1)
        assertIs<ReviewState.Completed>(vm.currentState.review)
    }

    @Test
    fun `completed session tracks known and unknown counts correctly`() = runTest {
        val vm = createViewModel() // 2 words
        vm.startSession(ReviewSource.DueCards)
        vm.reviewWord(quality = 1) // correct → knownCount=1, advance to index 1
        vm.reviewWord(quality = 0) // incorrect → unknownCount=1, last card → Completed
        val state = vm.currentState.review
        assertIs<ReviewState.Completed>(state)
        assertEquals(1, state.knownCount)
        assertEquals(1, state.unknownCount)
    }

    // ---------------------------------------------------------------------------
    // Session completion — flush
    // ---------------------------------------------------------------------------

    @Test
    fun `completing session by reviewing last word flushes the sync queue`() = runTest {
        dueWords = listOf(testWord(1))
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards) // flush call 1 (dequeueAll = 1)
        vm.reviewWord(quality = 1)             // last card → completeSession → flush call 2
        // Two flush invocations: startSession + completeSession
        assertEquals(2, syncRepo.dequeueAllCallCount)
    }

    @Test
    fun `completing session transitions to Completed and flush was called`() = runTest {
        dueWords = listOf(testWord(1))
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        val dequeueCountBeforeComplete = syncRepo.dequeueAllCallCount
        vm.reviewWord(quality = 1) // triggers completeSession
        assertIs<ReviewState.Completed>(vm.currentState.review)
        assertTrue(syncRepo.dequeueAllCallCount > dequeueCountBeforeComplete)
    }

    // ---------------------------------------------------------------------------
    // abandonSession — flush
    // ---------------------------------------------------------------------------

    @Test
    fun `abandonSession resets review state to Idle`() = runTest {
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        assertIs<ReviewState.Active>(vm.currentState.review)
        vm.abandonSession()
        assertIs<ReviewState.Idle>(vm.currentState.review)
    }

    @Test
    fun `abandonSession before startSession is a no-op`() = runTest {
        val vm = createViewModel()
        vm.abandonSession() // no session context — should not throw
        assertIs<ReviewState.Idle>(vm.currentState.review)
    }

    @Test
    fun `abandonSession flushes the sync queue`() = runTest {
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards) // flush call 1 (dequeueAll = 1)
        val dequeueBeforeAbandon = syncRepo.dequeueAllCallCount
        vm.abandonSession()                    // flush call 2
        assertTrue(syncRepo.dequeueAllCallCount > dequeueBeforeAbandon)
    }

    @Test
    fun `abandonSession without active session does not flush`() = runTest {
        val vm = createViewModel()
        // abandonSession is a no-op when sessionContext is null (never started)
        vm.abandonSession()
        assertEquals(0, syncRepo.dequeueAllCallCount)
    }

    // ---------------------------------------------------------------------------
    // flipCard
    // ---------------------------------------------------------------------------

    @Test
    fun `flipCard toggles isFlipped`() = runTest {
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        assertFalse((vm.currentState.review as ReviewState.Active).isFlipped)
        vm.flipCard()
        assertTrue((vm.currentState.review as ReviewState.Active).isFlipped)
        vm.flipCard()
        assertFalse((vm.currentState.review as ReviewState.Active).isFlipped)
    }

    // ---------------------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------------------

    @Test
    fun `navigateForward increments currentIndex and resets isFlipped`() = runTest {
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        vm.flipCard()
        vm.navigateForward()
        val state = vm.currentState.review as ReviewState.Active
        assertEquals(1, state.currentIndex)
        assertFalse(state.isFlipped)
    }

    @Test
    fun `navigateBack decrements currentIndex and resets isFlipped`() = runTest {
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        vm.navigateForward()
        vm.flipCard()
        vm.navigateBack()
        val state = vm.currentState.review as ReviewState.Active
        assertEquals(0, state.currentIndex)
        assertFalse(state.isFlipped)
    }

    @Test
    fun `navigateBack at index 0 is a no-op`() = runTest {
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        vm.navigateBack()
        assertEquals(0, (vm.currentState.review as ReviewState.Active).currentIndex)
    }

    @Test
    fun `navigateForward at last index is a no-op`() = runTest {
        dueWords = listOf(testWord(1))
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        vm.navigateForward()
        assertEquals(0, (vm.currentState.review as ReviewState.Active).currentIndex)
    }

    // ---------------------------------------------------------------------------
    // deleteWord
    // ---------------------------------------------------------------------------

    @Test
    fun `deleteWord removes word from list and stays Active`() = runTest {
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        vm.deleteWord(1)
        val state = vm.currentState.review
        assertIs<ReviewState.Active>(state)
        assertEquals(1, state.words.size)
        assertEquals(2, state.words.first().id)
    }

    @Test
    fun `deleteWord adjusts currentIndex when deleting at last position`() = runTest {
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        vm.navigateForward() // move to index 1
        vm.deleteWord(2)     // delete the word at index 1
        val state = vm.currentState.review as ReviewState.Active
        assertEquals(1, state.words.size)
        assertEquals(0, state.currentIndex) // coerced back to 0
    }

    @Test
    fun `deleteWord on last remaining word transitions to Completed`() = runTest {
        dueWords = listOf(testWord(1))
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        vm.deleteWord(1)
        assertIs<ReviewState.Completed>(vm.currentState.review)
    }

    @Test
    fun `deleteWord failure does not modify word list`() = runTest {
        deleteResult = Try.failure(RuntimeException("fail"))
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        vm.deleteWord(1)
        val state = vm.currentState.review
        assertIs<ReviewState.Active>(state)
        assertEquals(2, state.words.size)
    }

    // ---------------------------------------------------------------------------
    // updateWord
    // ---------------------------------------------------------------------------

    @Test
    fun `updateWord replaces the matching word in the list`() = runTest {
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        val updated = testWord(1).copy(originalWord = "updated")
        vm.updateWord(updated)
        val state = vm.currentState.review as ReviewState.Active
        assertEquals("updated", state.words.first { it.id == 1 }.originalWord)
    }

    @Test
    fun `updateWord failure does not modify word list`() = runTest {
        updateResult = Try.failure(RuntimeException("fail"))
        val vm = createViewModel()
        vm.startSession(ReviewSource.DueCards)
        vm.updateWord(testWord(1).copy(originalWord = "should-not-appear"))
        val state = vm.currentState.review as ReviewState.Active
        assertEquals("word1", state.words.first { it.id == 1 }.originalWord)
    }

    // ---------------------------------------------------------------------------
    // acknowledgeCompletion
    // ---------------------------------------------------------------------------

    @Test
    fun `acknowledgeCompletion emits SessionComplete and resets to Idle`() = runTest {
        dueWords = listOf(testWord(1)) // single word for quick completion
        val vm = createViewModel()
        vm.effects.test {
            vm.startSession(ReviewSource.DueCards)
            vm.reviewWord(quality = 1) // last card → Completed
            vm.acknowledgeCompletion()
            assertEquals(ReviewEffect.SessionComplete, awaitItem())
        }
        assertIs<ReviewState.Idle>(vm.currentState.review)
    }
}
