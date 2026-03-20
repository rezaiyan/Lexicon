package presentation.feature.study

import analytics.IAnalyticsTracker
import core.common.Try
import domain.analytics.model.ReviewEventParams
import domain.analytics.repository.IAnalyticsRecorder
import domain.analytics.usecase.EndStudySessionUseCase
import domain.analytics.usecase.RecordReviewEventUseCase
import domain.analytics.usecase.StartStudySessionUseCase
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.GetCurrentLanguageUseCase
import domain.settings.usecase.GetReviewSettingsUseCase
import domain.streak.model.StreakData
import domain.streak.repository.IStreakRepository
import domain.streak.usecase.RecordStreakActivityUseCase
import domain.tts.model.TtsModelInfo
import domain.tts.model.TtsState
import domain.tts.repository.ITtsRepository
import domain.tts.usecase.SpeakWordUseCase
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import domain.word.usecase.DeleteWordUseCase
import domain.word.usecase.GetDueWordsUseCase
import domain.word.usecase.GetWordsByStageUseCase
import domain.word.usecase.ReviewWordUseCase
import domain.word.usecase.UpdateWordUseCase
import feature.study.ReviewSessionUseCases
import feature.study.ReviewViewModel
import feature.study.ReviewWordUseCases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import core.common.UiState
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReviewViewModelTest : ViewModelTestBase() {

    private fun testWord(id: Int) = Word(
        id = id,
        originalWord = "word$id",
        translation = "trans$id",
        description = "desc$id",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.GERMAN,
        nextReviewDate = 0L
    )

    private var dueWords: List<Word> = listOf(testWord(1), testWord(2))
    private var stageWords: List<Word> = listOf(testWord(3))
    private var deleteResult: Try<Unit> = Try.success(Unit)
    private var updateResult: Try<Unit> = Try.success(Unit)
    private val loggedEvents = mutableListOf<String>()

    private fun fakeWordRepo() = object : IWordRepository {
        override fun getDueCards(): Flow<List<Word>> = flowOf(dueWords)
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(stageWords)
        override suspend fun deleteWord(id: Int): Try<Unit> = deleteResult
        override suspend fun updateWord(word: Word): Try<Unit> = updateResult
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
        override suspend fun getModelInfo(
            languageCode: String,
            displayName: String,
        ): Try<TtsModelInfo> =
            Try.success(TtsModelInfo(languageCode, displayName, false, 0L))
        override suspend fun deleteModel(languageCode: String): Try<Unit> = Try.success(Unit)
    }

    private fun fakeAnalytics() = object : IAnalyticsTracker {
        override fun logScreenView(screenName: String) {}
        override fun logEvent(eventName: String, parameters: Map<String, Any>?) {
            loggedEvents += eventName
        }
        override fun logWordReviewed(rating: Int, wordLevel: Int, wasCorrect: Boolean) {
            loggedEvents += "word_reviewed"
        }
        override fun logReviewSessionStart(cardCount: Int) {
            loggedEvents += "review_session_start"
        }
        override fun logReviewSessionComplete(
            cardsReviewed: Int,
            durationMs: Long,
            perfectCount: Int,
        ) {}
        override fun logWordsImported(count: Int, method: String) {}
        override fun logWordMastered(level: Int) {}
        override fun logStreakUpdated(days: Int, isNewRecord: Boolean) {}
        override fun logDailyGoalCompleted(cardsTarget: Int, cardsActual: Int) {}
        override fun logThemeChanged(themeMode: String, isDark: Boolean) {}
        override fun logLanguageChanged(language: String) {}
        override fun setUserProperty(name: String, value: String) {}
        override fun updateUserProgress(totalWords: Int, matureWords: Int, currentStreak: Int) {}
        override fun logError(error: Throwable, context: String?) {}
        override fun logNonFatalError(message: String, additionalInfo: Map<String, Any>?) {}
    }

    private fun fakeAnalyticsRecorder() = object : IAnalyticsRecorder {
        override suspend fun startSession(sessionId: String, reviewType: String, startedAt: Long) = Try.success(Unit)
        override suspend fun endSession(sessionId: String, endedAt: Long, durationMs: Long, totalCards: Int, correctCount: Int, incorrectCount: Int, completedNormally: Boolean) = Try.success(Unit)
        override suspend fun recordReviewEvent(params: ReviewEventParams) = Try.success(Unit)
    }


    private fun createViewModel(): ReviewViewModel {
        val wordRepo = fakeWordRepo()
        val settingsRepo = fakeSettingsRepo()
        val ttsRepo = fakeTtsRepo()
        val recorder = fakeAnalyticsRecorder()
        return ReviewViewModel(
            wordUseCases = ReviewWordUseCases(
                getDueWords = GetDueWordsUseCase(wordRepo),
                getWordsByStage = GetWordsByStageUseCase(wordRepo),
                reviewWord = ReviewWordUseCase(wordRepo, GetReviewSettingsUseCase()),
                updateWord = UpdateWordUseCase(wordRepo),
                deleteWord = DeleteWordUseCase(
                    wordRepo, fakes.FakeWidgetRefresher(), fakes.fakeGetDailyWidgetDataUseCase(wordRepo)
                ),
            ),
            sessionUseCases = ReviewSessionUseCases(
                startSession = StartStudySessionUseCase(recorder),
                endSession = EndStudySessionUseCase(recorder),
                recordEvent = RecordReviewEventUseCase(recorder),
                recordStreak = RecordStreakActivityUseCase(fakeStreakRepo()),
                getSettings = GetReviewSettingsUseCase(),
            ),
            speakWordUseCase = SpeakWordUseCase(ttsRepo, GetCurrentLanguageUseCase(settingsRepo)),
            analyticsTracker = fakeAnalytics(),
            ttsRepository = ttsRepo,
        )
    }

    @Test
    fun `startDueReview loads due words`() = runTest {
        val vm = createViewModel()
        vm.startDueReview()
        val state = vm.currentState.review.wordListState
        assertIs<UiState.Loaded<List<Word>>>(state)
        assertEquals(2, state.value.size)
    }

    @Test
    fun `loadWordsByStage loads stage words`() = runTest {
        val vm = createViewModel()
        vm.loadWordsByStage(LearningStage.LEVEL_0_FRESH)
        val state = vm.currentState.review.wordListState
        assertIs<UiState.Loaded<List<Word>>>(state)
        assertEquals(1, state.value.size)
    }

    @Test
    fun `deleteWord removes word from review list`() = runTest {
        val vm = createViewModel()
        vm.startDueReview()
        vm.deleteWord(1)
        val state = vm.currentState.review.wordListState
        assertIs<UiState.Loaded<List<Word>>>(state)
        assertEquals(1, state.value.size)
        assertEquals(2, state.value.first().id)
    }

    @Test
    fun `deleteWord failure does not remove word`() = runTest {
        deleteResult = Try.failure(RuntimeException("fail"))
        val vm = createViewModel()
        vm.startDueReview()
        vm.deleteWord(1)
        val state = vm.currentState.review.wordListState
        assertIs<UiState.Loaded<List<Word>>>(state)
        assertEquals(2, state.value.size)
    }

    @Test
    fun `updateWord replaces word in review list`() = runTest {
        val vm = createViewModel()
        vm.startDueReview()
        val updatedWord = testWord(1).copy(originalWord = "updated")
        vm.updateWord(updatedWord)
        val state = vm.currentState.review.wordListState
        assertIs<UiState.Loaded<List<Word>>>(state)
        assertEquals("updated", state.value.first { it.id == 1 }.originalWord)
    }

    @Test
    fun `onReviewSessionComplete resets review state`() = runTest {
        val vm = createViewModel()
        // Don't load words first — recordActivity calls logNetwork which uses
        // android.util.Log.d (unmocked in unit tests). With count=0, recordActivity is skipped.
        vm.onReviewSessionComplete()
        assertIs<UiState.Loading>(vm.currentState.review.wordListState)
    }

    @Test
    fun `reviewWord logs analytics`() = runTest {
        val vm = createViewModel()
        vm.reviewWord(testWord(1), quality = 1)
        assertTrue("word_reviewed" in loggedEvents)
    }

    @Test
    fun `startStageReview loads stage words and logs analytics`() = runTest {
        val vm = createViewModel()
        vm.startStageReview(LearningStage.LEVEL_0_FRESH)

        val state = vm.currentState.review.wordListState
        assertIs<UiState.Loaded<List<Word>>>(state)
        assertTrue("review_session_start" in loggedEvents)
    }

    @Test
    fun `updateWord failure does not modify review list`() = runTest {
        updateResult = Try.failure(RuntimeException("fail"))
        val vm = createViewModel()
        vm.startDueReview()

        val updatedWord = testWord(1).copy(originalWord = "should-not-appear")
        vm.updateWord(updatedWord)

        val state = vm.currentState.review.wordListState
        assertIs<UiState.Loaded<List<Word>>>(state)
        assertEquals("word1", state.value.first { it.id == 1 }.originalWord)
    }

    @Test
    fun `initial ttsState is Idle`() {
        val vm = createViewModel()
        assertEquals(TtsState.Idle, vm.currentState.ttsState)
    }
}
