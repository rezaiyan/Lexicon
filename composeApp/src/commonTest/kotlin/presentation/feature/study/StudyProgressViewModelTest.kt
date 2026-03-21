package presentation.feature.study

import analytics.IAnalyticsTracker
import core.common.Try
import fakes.FakePerformanceTracer
import feature.study.StudyProgressViewModel
import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.UserFeatureAccess
import domain.auth.repository.IAuthRepository
import domain.auth.model.AuthUser
import domain.auth.usecase.GetFeatureAccessUseCase
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
import domain.word.usecase.EvaluateProgressUseCase
import domain.word.usecase.GetProgressStatsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import core.common.UiState
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StudyProgressViewModelTest : ViewModelTestBase() {

    private fun fakeWordRepo() = object : IWordRepository {
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
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

    private fun fakeAuthRepo() = object : IAuthRepository {
        override suspend fun loginWithGoogle(idToken: String): Try<AuthUser> =
            Try.failure(RuntimeException(""))
        override suspend fun loginWithApple(
            idToken: String,
            fullName: String?,
            appleUserId: String,
        ): Try<AuthUser> = Try.failure(RuntimeException(""))
        override suspend fun logout(): Try<Unit> = Try.success(Unit)
        override suspend fun deleteAccount(): Try<Unit> = Try.success(Unit)
        override suspend fun getAccessToken(): String? = null
        override suspend fun isAuthenticated(): Boolean = false
        override fun isAuthenticatedAsFlow(): Flow<Boolean> = flowOf(false)
        override fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse> = flowOf(
            FeatureAccessResponse(
                FeatureFlags(),
                UserFeatureAccess(hasPremiumAccess = false),
            )
        )
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
        override fun logLanguageChanged(language: String) {}
        override fun setUserProperty(name: String, value: String) {}
        override fun updateUserProgress(totalWords: Int, matureWords: Int, currentStreak: Int) {}
        override fun logError(error: Throwable, context: String?) {}
        override fun logNonFatalError(message: String, additionalInfo: Map<String, Any>?) {}
    }

    private fun createViewModel(): StudyProgressViewModel {
        val wordRepo = fakeWordRepo()
        val settingsRepo = fakeSettingsRepo()
        val notifRepo = fakeNotifRepo()
        return StudyProgressViewModel(
            getProgressStatsUseCase = GetProgressStatsUseCase(wordRepo),
            evaluateProgressUseCase = EvaluateProgressUseCase(),
            scheduleNotificationsUseCase = ScheduleNotificationsUseCase(notifRepo, settingsRepo),
            analyticsTracker = fakeAnalytics(),
            performanceTracer = FakePerformanceTracer(),
            getFeatureAccessUseCase = GetFeatureAccessUseCase(fakeAuthRepo()),
        )
    }

    @Test
    fun `initial progress state is Loading`() {
        val vm = createViewModel()
        assertIs<UiState.Loading>(vm.currentState.progress)
    }

    @Test
    fun `initial hasPremiumAccess is false`() = runTest {
        val vm = createViewModel()
        assertEquals(false, vm.currentState.hasPremiumAccess)
    }
}
