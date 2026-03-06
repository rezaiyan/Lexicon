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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import platform.IAppVersionProvider
import feature.settings.NotificationPermissionMonitor
import feature.settings.SettingsViewModel
import presentation.ViewModelTestBase
import feature.settings.model.DialogState
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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
        override suspend fun setLanguage(language: Language) { lastSetLanguage = language }
        override fun getThemeMode(): Flow<ThemeMode> = themeModeFlow
        override suspend fun setThemeMode(mode: ThemeMode) { lastSetThemeMode = mode }
        override suspend fun getLastInsightDate(): String? = null
        override suspend fun getCachedInsight(): String? = null
        override suspend fun updateDailyInsight(date: String, insight: String) {}
        override suspend fun getLastInsightDismissedTime(): Long = 0L
        override suspend fun setLastInsightDismissedTime(timestamp: Long) {}
        override suspend fun clearInsightData() {}
        override suspend fun clearSettings() {}
        override fun getNotificationsEnabled(): Flow<Boolean> = notificationsEnabledFlow
        override suspend fun setNotificationsEnabled(enabled: Boolean) { lastSetNotificationsEnabled = enabled }
        override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setReviewRemindersEnabled(enabled: Boolean) {}
        override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setMotivationalMessagesEnabled(enabled: Boolean) {}
        override suspend fun getDailyReminderTime(): String = "09:00"
        override suspend fun setDailyReminderTime(time: String) {}
        override suspend fun getMinimumDueCards(): Int = 5
        override suspend fun setMinimumDueCards(count: Int) {}
    }

    private fun fakeNotificationRepo() = object : INotificationRepository {
        override suspend fun scheduleReviewReminder(dueCount: Int, title: String, message: String, delayMinutes: Int) {}
        override suspend fun areNotificationsEnabled(): Boolean = systemNotificationsEnabled
        override suspend fun requestNotificationPermission(): Boolean = requestPermissionResult
        override suspend fun wasNotificationPermissionDenied(): Boolean = false
        override suspend fun openNotificationSettings() {}
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
        override fun logAiInsightGenerated(usedLocal: Boolean, totalWords: Int) {}
        override fun logThemeChanged(themeMode: String, isDark: Boolean) { loggedEvents += "theme_changed" }
        override fun logLanguageChanged(language: String) { loggedEvents += "language_changed" }
        override fun setUserProperty(name: String, value: String) {}
        override fun updateUserProgress(totalWords: Int, matureWords: Int, currentStreak: Int) {}
        override fun logError(error: Throwable, context: String?) {}
        override fun logNonFatalError(message: String, additionalInfo: Map<String, Any>?) {}
    }

    private fun createViewModel(): SettingsViewModel {
        val settingsRepo = fakeSettingsRepo()
        val notifRepo = fakeNotificationRepo()
        return SettingsViewModel(
            notificationRepository = notifRepo,
            setLanguageUseCase = SetLanguageUseCase(settingsRepo),
            setThemeModeUseCase = SetThemeModeUseCase(settingsRepo),
            setNotificationsEnabledUseCase = SetNotificationsEnabledUseCase(settingsRepo),
            requestNotificationPermissionUseCase = RequestNotificationPermissionUseCase(notifRepo),
            openNotificationSettingsUseCase = OpenNotificationSettingsUseCase(notifRepo),
            analyticsTracker = fakeAnalytics(),
            notificationPermissionMonitor = NotificationPermissionMonitor(notifRepo),
            settingsRepository = settingsRepo,
            authRepository = fakeAuthRepo(),
            appVersionProvider = fakeAppVersionProvider()
        )
    }

    @Test
    fun `initial dialog state is None`() {
        val vm = createViewModel()
        assertEquals(DialogState.None, vm.currentState.dialog)
    }

    @Test
    fun `showDialog sets dialog state`() {
        val vm = createViewModel()
        vm.showDialog(DialogState.LanguageSelection)
        assertEquals(DialogState.LanguageSelection, vm.currentState.dialog)
    }

    @Test
    fun `dismissDialog clears dialog state`() {
        val vm = createViewModel()
        vm.showDialog(DialogState.ThemeSelection)
        vm.dismissDialog()
        assertEquals(DialogState.None, vm.currentState.dialog)
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
    fun `setNotificationsEnabled when system disabled shows permission dialog`() = runTest {
        systemNotificationsEnabled = false
        val vm = createViewModel()
        vm.setNotificationsEnabled(true)
        assertEquals(DialogState.NotificationPermission, vm.currentState.dialog)
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
