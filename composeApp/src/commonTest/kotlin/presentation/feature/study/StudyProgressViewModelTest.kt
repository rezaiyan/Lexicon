package presentation.feature.study

import domain.word.add.model.AddWordsOutcome
import analytics.IAnalyticsTracker
import core.common.Try
import fakes.FakePerformanceTracer
import feature.study.StudyProgressViewModel
import feature.study.StudyTagUseCases
import feature.study.StudyFocusUseCases
import feature.study.model.ProgressScreenState
import feature.study.util.NotificationText
import domain.focus.model.LearningFocus
import domain.focus.usecase.AcknowledgeFocusIntroUseCase
import domain.focus.usecase.DismissFocusNudgeUseCase
import domain.focus.usecase.ObserveStudyFocusUseCase
import domain.focus.usecase.SetLearningFocusUseCase
import fakes.FakeLearningFocusRepository
import domain.notifications.repository.INotificationRepository
import domain.notifications.usecase.ScheduleNotificationsUseCase
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import domain.settings.usecase.GetSkipTagSelectorUseCase
import domain.settings.usecase.SetSkipTagSelectorUseCase
import domain.tag.model.Tag
import domain.tag.repository.ITagRepository
import domain.word.usecase.EvaluateProgressUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import core.common.UiState
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StudyProgressViewModelTest : ViewModelTestBase() {

    private fun fakeWordRepo(allWords: Flow<List<Word>> = emptyFlow()) = object : IWordRepository {
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCardsByTag(tagId: Long): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
        override fun getAllWords(): Flow<List<Word>> = allWords
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun addWords(words: List<Word>): Try<AddWordsOutcome> = Try.success(AddWordsOutcome(words.size, 0))
        override suspend fun uploadPendingWords(): Try<Int> = Try.success(0)
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
        override suspend fun getNextDueAt(): Try<Long?> = Try.success(null)
        override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
        override suspend fun updateWordLocal(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun batchSyncWords(words: List<Word>): Try<Unit> = Try.success(Unit)
    }

    private fun fakeSettingsRepo() = object : ISettingsRepository {
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

    private fun fakeNotifRepo() = object : INotificationRepository {
        override suspend fun scheduleReviewReminder(
            dueCount: Int,
            title: String,
            message: String,
            delayMinutes: Int,
        ): Try<Unit> = Try.success(Unit)
        override suspend fun areNotificationsEnabled(): Try<Boolean> = Try.success(true)
        override suspend fun requestNotificationPermission(): Try<Boolean> = Try.success(true)
        override suspend fun wasNotificationPermissionDenied(): Try<Boolean> = Try.success(false)
        override suspend fun openNotificationSettings(): Try<Unit> = Try.success(Unit)
    }

    private fun fakeAnalytics() = object : IAnalyticsTracker {
        override fun logScreenView(screenName: String) {}
        override fun logEvent(eventName: String, parameters: Map<String, Any>?) {}
        override fun logWordReviewed(rating: Int, wordLevel: Int, wasCorrect: Boolean) {}
        override fun logReviewSessionStart(cardCount: Int) {}
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
        override fun setUserProperty(name: String, value: String) {}
        override fun updateUserProgress(totalWords: Int, matureWords: Int, currentStreak: Int) {}
        override fun logError(error: Throwable, context: String?) {}
        override fun logNonFatalError(message: String, additionalInfo: Map<String, Any>?) {}
    }

    private fun fakeTagRepo(tags: List<Tag> = emptyList()) = object : ITagRepository {
        override fun getTags(): Flow<List<Tag>> = flowOf(tags)
        override fun getTagsByLevel(): Flow<Map<Int, List<Tag>>> = flowOf(emptyMap())
        override fun getDueTags(): Flow<List<Tag>> = flowOf(emptyList())
        override suspend fun createTag(name: String): Try<Tag> = Try.success(Tag(1L, name, 0L, 0L, 0L))
        override suspend fun renameTag(id: Long, name: String): Try<Tag> = Try.success(Tag(1L, name, 0L, 0L, 0L))
        override suspend fun deleteTag(id: Long): Try<Unit> = Try.success(Unit)
        override suspend fun assignWordTags(wordId: Long, tagIds: List<Long>): Try<Unit> = Try.success(Unit)
        override suspend fun batchAssignWordTags(wordIds: List<Long>, tagIds: List<Long>): Try<Unit> = Try.success(Unit)
        override suspend fun syncTagsFromRemote(): Try<Unit> = Try.success(Unit)
    }

    private val now = 1_000_000_000L
    private lateinit var focusRepo: FakeLearningFocusRepository

    private fun word(id: Int, language: Language, tags: List<Long> = emptyList()) = Word(
        id = id,
        originalWord = "w$id",
        translation = "t$id",
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = language,
        nextReviewDate = now - 1,
        tagIds = tags,
    )

    private val mixedWords = buildList {
        add(word(1, Language.GERMAN))
        repeat(10) { add(word(100 + it, Language.SPANISH)) }
    }

    private fun createViewModel(
        tags: List<Tag> = emptyList(),
        words: Flow<List<Word>> = emptyFlow(),
        preference: LearningFocus? = null,
    ): StudyProgressViewModel {
        val wordRepo = fakeWordRepo(words)
        val settingsRepo = fakeSettingsRepo()
        val notifRepo = fakeNotifRepo()
        focusRepo = FakeLearningFocusRepository(preference)
        return StudyProgressViewModel(
            evaluateProgressUseCase = EvaluateProgressUseCase(),
            scheduleNotificationsUseCase = ScheduleNotificationsUseCase(notifRepo, settingsRepo),
            analyticsTracker = fakeAnalytics(),
            performanceTracer = FakePerformanceTracer(),
            tagUseCases = StudyTagUseCases(
                getSkipTagSelector = GetSkipTagSelectorUseCase(settingsRepo),
                setSkipTagSelector = SetSkipTagSelectorUseCase(settingsRepo),
            ),
            focusUseCases = StudyFocusUseCases(
                observeStudyFocus = ObserveStudyFocusUseCase(wordRepo, fakeTagRepo(tags), focusRepo) { now },
                setLearningFocus = SetLearningFocusUseCase(focusRepo),
                dismissFocusNudge = DismissFocusNudgeUseCase(focusRepo) { now },
                acknowledgeFocusIntro = AcknowledgeFocusIntroUseCase(focusRepo),
            ),
            notificationTextResolver = { NotificationText(title = "title", message = "message") },
        )
    }

    @Test
    fun `initial progress state is Loading`() {
        val vm = createViewModel()
        assertIs<UiState.Loading>(vm.currentState.progress)
    }

    @Test
    fun `tags are counted from focused words`() = runTest {
        val tags = listOf(
            Tag(id = 1L, name = "Travel", wordCount = 99L, createdAt = 0L, updatedAt = 0L),
            Tag(id = 2L, name = "Work", wordCount = 99L, createdAt = 0L, updatedAt = 0L),
        )
        val words = flowOf(
            listOf(word(1, Language.GERMAN, tags = listOf(1L)), word(2, Language.GERMAN, tags = listOf(1L)))
        )
        val vm = createViewModel(tags = tags, words = words)
        // single language resolves to All focus: empty tags kept, counts recomputed from words
        assertEquals(listOf(1L to 2L, 2L to 0L), vm.currentState.tags.map { it.id to it.wordCount })
    }

    @Test
    fun `two languages expose switcher focus intro and nudge`() = runTest {
        val vm = createViewModel(words = flowOf(mixedWords), preference = LearningFocus.Single(Language.GERMAN))
        val state = vm.currentState
        assertEquals(LearningFocus.Single(Language.GERMAN), state.focus)
        assertTrue(state.showFocusSwitcher)
        assertTrue(state.showIntro)
        assertEquals(Language.SPANISH, state.nudge?.language)
        assertEquals(1, assertIs<UiState.Loaded<ProgressScreenState>>(state.progress).value.progressStats.totalWords)
    }

    @Test
    fun `selectFocus rescopes stats and acknowledges intro`() = runTest {
        val vm = createViewModel(words = flowOf(mixedWords), preference = LearningFocus.Single(Language.GERMAN))
        vm.selectFocus(LearningFocus.Single(Language.SPANISH))
        val progress = assertIs<UiState.Loaded<ProgressScreenState>>(vm.currentState.progress)
        assertEquals(10, progress.value.progressStats.totalWords)
        assertEquals(LearningFocus.Single(Language.SPANISH), focusRepo.preference.value)
        assertFalse(vm.currentState.showIntro)
    }

    @Test
    fun `dismissNudge hides nudge`() = runTest {
        val vm = createViewModel(words = flowOf(mixedWords), preference = LearningFocus.Single(Language.GERMAN))
        vm.dismissNudge()
        assertNull(vm.currentState.nudge)
    }

    @Test
    fun `acknowledgeIntro hides intro`() = runTest {
        val vm = createViewModel(words = flowOf(mixedWords))
        vm.acknowledgeIntro()
        assertFalse(vm.currentState.showIntro)
    }

    @Test
    fun `single language hides switcher`() = runTest {
        val vm = createViewModel(words = flowOf(listOf(word(1, Language.GERMAN))))
        assertFalse(vm.currentState.showFocusSwitcher)
        assertEquals(LearningFocus.All, vm.currentState.focus)
    }
}
