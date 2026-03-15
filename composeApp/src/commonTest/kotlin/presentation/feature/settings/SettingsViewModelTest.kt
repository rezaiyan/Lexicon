package presentation.feature.settings

import analytics.IAnalyticsTracker
import core.common.Try
import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.UserFeatureAccess
import domain.auth.repository.IAuthRepository
import domain.auth.model.AuthUser
import domain.notifications.repository.INotificationRepository
import domain.notifications.usecase.OpenNotificationSettingsUseCase
import domain.notifications.usecase.RequestNotificationPermissionUseCase
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.SetLanguageUseCase
import domain.settings.usecase.SetNotificationsEnabledUseCase
import domain.settings.usecase.SetThemeModeUseCase
import domain.tts.model.TtsModelInfo
import domain.tts.repository.ITtsRepository
import domain.tts.model.TtsState
import domain.tts.usecase.DeleteTtsModelUseCase
import domain.tts.usecase.GetTtsModelsInfoUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import platform.IAppVersionProvider
import feature.settings.NotificationPermissionMonitor
import feature.settings.SettingsViewModel
import presentation.ViewModelTestBase
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest : ViewModelTestBase() {

    private var languageFlow = MutableStateFlow(Language.ENGLISH)
    private var themeModeFlow = MutableStateFlow(ThemeMode.AUTO)
    private var notificationsEnabledFlow = MutableStateFlow(true)
    private var systemNotificationsEnabled = true
    private var requestPermissionResult = true
    private val loggedEvents = mutableListOf<String>()
    private var lastSetLanguage: Language? = null
    private var lastSetThemeMode: ThemeMode? = null
    private var lastSetNotificationsEnabled: Boolean? = null

    private fun fakeSettingsRepo() = object : ISettingsRepository {
        override fun getLanguage(): Flow<Language> = languageFlow
        override suspend fun setLanguage(language: Language): Try<Unit> {
            lastSetLanguage = language
            return Try.success(Unit)
        }
        override fun getThemeMode(): Flow<ThemeMode> = themeModeFlow
        override suspend fun setThemeMode(mode: ThemeMode): Try<Unit> {
            lastSetThemeMode = mode
            return Try.success(Unit)
        }
        override suspend fun clearSettings(): Try<Unit> = Try.success(Unit)
        override fun getNotificationsEnabled(): Flow<Boolean> = notificationsEnabledFlow
        override suspend fun setNotificationsEnabled(enabled: Boolean): Try<Unit> {
            lastSetNotificationsEnabled = enabled
            return Try.success(Unit)
        }
        override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setReviewRemindersEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setMotivationalMessagesEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override suspend fun getDailyReminderTime(): Try<String> = Try.success("09:00")
        override suspend fun setDailyReminderTime(time: String): Try<Unit> = Try.success(Unit)
        override suspend fun getMinimumDueCards(): Try<Int> = Try.success(5)
        override suspend fun setMinimumDueCards(count: Int): Try<Unit> = Try.success(Unit)
    }

    private fun fakeNotificationRepo() = object : INotificationRepository {
        override suspend fun scheduleReviewReminder(
            dueCount: Int,
            title: String,
            message: String,
            delayMinutes: Int,
        ): Try<Unit> = Try.success(Unit)
        override suspend fun areNotificationsEnabled(): Try<Boolean> = Try.success(systemNotificationsEnabled)
        override suspend fun requestNotificationPermission(): Try<Boolean> = Try.success(requestPermissionResult)
        override suspend fun wasNotificationPermissionDenied(): Try<Boolean> = Try.success(false)
        override suspend fun openNotificationSettings(): Try<Unit> = Try.success(Unit)
    }

    private fun fakeAuthRepo() = object : IAuthRepository {
        override suspend fun loginWithGoogle(idToken: String): Try<AuthUser> = Try.failure(RuntimeException("not implemented"))
        override suspend fun loginWithApple(idToken: String, fullName: String?, appleUserId: String): Try<AuthUser> = Try.failure(RuntimeException("not implemented"))
        override suspend fun logout(): Try<Unit> = Try.success(Unit)
        override suspend fun deleteAccount(): Try<Unit> = Try.success(Unit)
        override suspend fun getAccessToken(): String? = null
        override suspend fun isAuthenticated(): Boolean = false
        override fun isAuthenticatedAsFlow(): Flow<Boolean> = flowOf(false)
        override fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse> = flowOf(
            FeatureAccessResponse(
                featureFlags = FeatureFlags(),
                userAccess = UserFeatureAccess(hasPremiumAccess = false)
            )
        )
    }

    private fun fakeAppVersionProvider() = object : IAppVersionProvider {
        override fun getVersion(): String = "1.0.0"
    }

    private fun fakeAnalytics() = object : IAnalyticsTracker {
        override fun logScreenView(screenName: String) {}
        override fun logEvent(eventName: String, parameters: Map<String, Any>?) { loggedEvents += eventName }
        override fun logWordReviewed(rating: Int, wordLevel: Int, wasCorrect: Boolean) {}
        override fun logReviewSessionStart(cardCount: Int) {}
        override fun logReviewSessionComplete(cardsReviewed: Int, durationMs: Long, perfectCount: Int) {}
        override fun logWordsImported(count: Int, method: String) {}
        override fun logWordMastered(level: Int) {}
        override fun logStreakUpdated(days: Int, isNewRecord: Boolean) {}
        override fun logDailyGoalCompleted(cardsTarget: Int, cardsActual: Int) {}
        override fun logThemeChanged(themeMode: String, isDark: Boolean) { loggedEvents += "theme_changed" }
        override fun logLanguageChanged(language: String) { loggedEvents += "language_changed" }
        override fun setUserProperty(name: String, value: String) {}
        override fun updateUserProgress(totalWords: Int, matureWords: Int, currentStreak: Int) {}
        override fun logError(error: Throwable, context: String?) {}
        override fun logNonFatalError(message: String, additionalInfo: Map<String, Any>?) {}
    }

    private fun fakeTtsRepo() = object : ITtsRepository {
        override val ttsState: StateFlow<TtsState> = MutableStateFlow(TtsState.Idle)
        override suspend fun speak(text: String, languageCode: String): Try<Unit> = Try.success(Unit)
        override suspend fun stop(): Try<Unit> = Try.success(Unit)
        override suspend fun isModelDownloaded(languageCode: String): Try<Boolean> = Try.success(false)
        override suspend fun downloadModel(languageCode: String): Flow<Float> = flowOf(1.0f)
        override fun isLanguageSupported(languageCode: String): Boolean = true
        override fun getSupportedLanguageCodes(): Set<String> = setOf("en")
        override suspend fun getModelInfo(languageCode: String, displayName: String): Try<TtsModelInfo> =
            Try.success(TtsModelInfo(languageCode, displayName, false, 0L))
        override suspend fun deleteModel(languageCode: String): Try<Unit> = Try.success(Unit)
    }

    private fun createViewModel(): SettingsViewModel {
        val settingsRepo = fakeSettingsRepo()
        val notifRepo = fakeNotificationRepo()
        val ttsRepo = fakeTtsRepo()
        return SettingsViewModel(
            notificationRepository = notifRepo,
            setLanguageUseCase = SetLanguageUseCase(settingsRepo),
            setThemeModeUseCase = SetThemeModeUseCase(settingsRepo),
            setNotificationsEnabledUseCase = SetNotificationsEnabledUseCase(settingsRepo),
            requestNotificationPermissionUseCase = RequestNotificationPermissionUseCase(notifRepo),
            openNotificationSettingsUseCase = OpenNotificationSettingsUseCase(notifRepo),
            analyticsTracker = fakeAnalytics(),
            notificationPermissionMonitor = NotificationPermissionMonitor(notifRepo),
            getTtsModelsInfoUseCase = GetTtsModelsInfoUseCase(ttsRepo),
            deleteTtsModelUseCase = DeleteTtsModelUseCase(ttsRepo),
            settingsRepository = settingsRepo,
            authRepository = fakeAuthRepo(),
            appVersionProvider = fakeAppVersionProvider()
        )
    }

    @Test
    fun `setLanguage delegates to use case`() = runTest {
        val vm = createViewModel()
        vm.setLanguage(Language.GERMAN)
        assertEquals(Language.GERMAN, lastSetLanguage)
    }

    @Test
    fun `setLanguage logs analytics`() = runTest {
        val vm = createViewModel()
        vm.setLanguage(Language.GERMAN)
        assertTrue(loggedEvents.contains("language_changed"))
    }

    @Test
    fun `setThemeMode delegates to use case`() = runTest {
        val vm = createViewModel()
        vm.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, lastSetThemeMode)
    }

    @Test
    fun `setThemeMode logs analytics`() = runTest {
        val vm = createViewModel()
        vm.setThemeMode(ThemeMode.DARK)
        assertTrue(loggedEvents.contains("theme_changed"))
    }

    @Test
    fun `setNotificationsEnabled delegates to use case`() = runTest {
        val vm = createViewModel()
        vm.setNotificationsEnabled(true)
        assertEquals(true, lastSetNotificationsEnabled)
    }

    @Test
    fun `settings state is built from repository flows`() = runTest {
        val vm = createViewModel()
        val screen = vm.currentState.screen
        assertEquals(Language.ENGLISH, screen.currentLanguage)
        assertEquals(ThemeMode.AUTO, screen.themeMode)
        assertEquals("1.0.0", screen.appVersion)
    }
}
