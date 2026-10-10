package presentation.feature.settings

import analytics.IAnalyticsTracker
import core.common.Try
import domain.notifications.repository.INotificationRepository
import domain.notifications.usecase.OpenNotificationSettingsUseCase
import domain.notifications.usecase.RequestNotificationPermissionUseCase
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.GetDailyGoalWordsUseCase
import domain.settings.usecase.SetDailyGoalWordsUseCase
import domain.settings.usecase.SetNotificationsEnabledUseCase
import domain.settings.usecase.SetReviewRemindersEnabledUseCase
import domain.settings.usecase.SetThemeModeUseCase
import domain.tts.model.TtsModelInfo
import domain.tts.repository.ITtsRepository
import domain.tts.model.TtsState
import domain.settings.usecase.SetTtsExpressivenessUseCase
import domain.settings.usecase.SetTtsVoiceUseCase
import domain.settings.usecase.SetTtsSpeechRateUseCase
import domain.tts.model.TtsSettings
import domain.tts.model.TtsVoice
import domain.tts.usecase.DeleteTtsModelUseCase
import domain.tts.usecase.DownloadTtsModelUseCase
import domain.tts.usecase.GetTtsModelsInfoUseCase
import domain.tts.usecase.SelectTtsVoiceUseCase
import domain.tts.usecase.SpeakWordUseCase
import domain.tts.usecase.StopSpeakingUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import platform.IAppVersionProvider
import feature.settings.NotificationPermissionMonitor
import feature.settings.SettingsViewModel
import presentation.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsViewModelTest : ViewModelTestBase() {

    private var themeModeFlow = MutableStateFlow(ThemeMode.AUTO)
    private var notificationsEnabledFlow = MutableStateFlow(true)
    private var systemNotificationsEnabled = true
    private var requestPermissionResult = true
    private val loggedEvents = mutableListOf<String>()
    private var lastSetThemeMode: ThemeMode? = null
    private var lastSetNotificationsEnabled: Boolean? = null
    private var lastSetReviewRemindersEnabled: Boolean? = null
    private var reviewRemindersEnabledFlow = MutableStateFlow(true)
    private var lastSetDailyGoalWords: Int? = null
    private var storedDailyGoalWords: Int = 10
    private var lastSetExpressiveness: Float? = null

    // TTS fake state
    private var modelDownloaded = false
    private var selectedVoiceId = "en_US-kristin-medium"
    private val selectVoiceCalls = mutableListOf<String>()
    private var downloadCalls = 0
    private var downloadShouldFail = false
    private val spokenTexts = mutableListOf<String>()
    private var speakGate: CompletableDeferred<Unit>? = null
    private var stopCalls = 0

    private fun fakeSettingsRepo() = object : ISettingsRepository {
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
        override fun getReviewRemindersEnabled(): Flow<Boolean> = reviewRemindersEnabledFlow
        override suspend fun setReviewRemindersEnabled(enabled: Boolean): Try<Unit> {
            lastSetReviewRemindersEnabled = enabled
            reviewRemindersEnabledFlow.value = enabled
            return Try.success(Unit)
        }
        override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setMotivationalMessagesEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override suspend fun getDailyReminderTime(): Try<String> = Try.success("09:00")
        override suspend fun setDailyReminderTime(time: String): Try<Unit> = Try.success(Unit)
        override suspend fun getMinimumDueCards(): Try<Int> = Try.success(5)
        override suspend fun setMinimumDueCards(count: Int): Try<Unit> = Try.success(Unit)
        override suspend fun getDailyGoalWords(): Try<Int> = Try.success(storedDailyGoalWords)
        override suspend fun setDailyGoalWords(count: Int): Try<Unit> {
            lastSetDailyGoalWords = count
            storedDailyGoalWords = count
            return Try.success(Unit)
        }
        override suspend fun setTtsExpressiveness(value: Float): Try<Unit> {
            lastSetExpressiveness = value
            return Try.success(Unit)
        }
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
        override fun setUserProperty(name: String, value: String) {}
        override fun updateUserProgress(totalWords: Int, matureWords: Int, currentStreak: Int) {}
        override fun logError(error: Throwable, context: String?) {}
        override fun logNonFatalError(message: String, additionalInfo: Map<String, Any>?) {}
    }

    private fun fakeTtsRepo() = object : ITtsRepository {
        override val ttsState: StateFlow<TtsState> = MutableStateFlow(TtsState.Idle)
        override suspend fun speak(text: String, languageCode: String): Try<Unit> {
            spokenTexts += text
            speakGate?.await()
            return Try.success(Unit)
        }
        override suspend fun stop(): Try<Unit> {
            stopCalls++
            return Try.success(Unit)
        }
        override suspend fun isModelDownloaded(languageCode: String): Try<Boolean> = Try.success(modelDownloaded)
        override suspend fun downloadModel(languageCode: String): Flow<Float> = flow {
            downloadCalls++
            emit(0.5f)
            if (downloadShouldFail) error("network down")
            modelDownloaded = true
            emit(1.0f)
        }
        override fun isLanguageSupported(languageCode: String): Boolean = true
        override fun getSupportedLanguageCodes(): Set<String> = setOf("en")
        override suspend fun getModelInfo(languageCode: String, displayName: String): Try<TtsModelInfo> =
            Try.success(
                TtsModelInfo(
                    languageCode = languageCode,
                    languageDisplayName = displayName,
                    isDownloaded = modelDownloaded,
                    sizeBytes = if (modelDownloaded) 100L else 0L,
                    voices = listOf(TtsVoice("en_US-kristin-medium", "Kristin", "US"), TtsVoice("en_GB-alan-medium", "Alan", "GB")),
                    selectedVoiceId = selectedVoiceId,
                    sampleText = SAMPLE,
                )
            )
        override suspend fun deleteModel(languageCode: String): Try<Unit> = Try.success(Unit)
        override suspend fun selectVoice(languageCode: String, voiceId: String): Try<Unit> {
            selectVoiceCalls += voiceId
            selectedVoiceId = voiceId
            modelDownloaded = false
            return Try.success(Unit)
        }
    }

    private fun createViewModel(): SettingsViewModel {
        val settingsRepo = fakeSettingsRepo()
        val notifRepo = fakeNotificationRepo()
        val ttsRepo = fakeTtsRepo()
        return SettingsViewModel(
            notificationRepository = notifRepo,
            setThemeModeUseCase = SetThemeModeUseCase(settingsRepo),
            setNotificationsEnabledUseCase = SetNotificationsEnabledUseCase(settingsRepo),
            setReviewRemindersEnabledUseCase = SetReviewRemindersEnabledUseCase(settingsRepo),
            requestNotificationPermissionUseCase = RequestNotificationPermissionUseCase(notifRepo),
            openNotificationSettingsUseCase = OpenNotificationSettingsUseCase(notifRepo),
            analyticsTracker = fakeAnalytics(),
            notificationPermissionMonitor = NotificationPermissionMonitor(notifRepo),
            getTtsModelsInfoUseCase = GetTtsModelsInfoUseCase(ttsRepo, settingsRepo),
            deleteTtsModelUseCase = DeleteTtsModelUseCase(ttsRepo),
            downloadTtsModelUseCase = DownloadTtsModelUseCase(ttsRepo),
            setTtsSpeechRateUseCase = SetTtsSpeechRateUseCase(settingsRepo),
            setTtsVoiceUseCase = SetTtsVoiceUseCase(settingsRepo),
            setTtsExpressivenessUseCase = SetTtsExpressivenessUseCase(settingsRepo),
            selectTtsVoiceUseCase = SelectTtsVoiceUseCase(ttsRepo),
            speakWordUseCase = SpeakWordUseCase(ttsRepo),
            stopSpeakingUseCase = StopSpeakingUseCase(ttsRepo),
            getDailyGoalWordsUseCase = GetDailyGoalWordsUseCase(settingsRepo),
            setDailyGoalWordsUseCase = SetDailyGoalWordsUseCase(settingsRepo),
            settingsRepository = settingsRepo,
            appVersionProvider = fakeAppVersionProvider()
        )
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
        assertEquals(ThemeMode.AUTO, screen.themeMode)
        assertEquals("1.0.0", screen.appVersion)
    }

    @Test
    fun `setReviewRemindersEnabled delegates to use case`() = runTest {
        val vm = createViewModel()
        vm.setReviewRemindersEnabled(false)
        assertEquals(false, lastSetReviewRemindersEnabled)
    }

    @Test
    fun `reviewRemindersEnabled default state is true`() = runTest {
        val vm = createViewModel()
        assertEquals(true, vm.currentState.screen.reviewRemindersEnabled)
    }

    @Test
    fun `dailyGoalWords loads from repository on init`() = runTest {
        storedDailyGoalWords = 20
        val vm = createViewModel()
        assertEquals(20, vm.currentState.dailyGoalWords)
    }

    @Test
    fun `setDailyGoalWords updates state and delegates to use case`() = runTest {
        val vm = createViewModel()
        vm.setDailyGoalWords(30)
        assertEquals(30, lastSetDailyGoalWords)
        assertEquals(30, vm.currentState.dailyGoalWords)
    }

    @Test
    fun `selectTtsModelVoice when model downloaded switches voice and downloads it`() = runTest {
        modelDownloaded = true
        val vm = createViewModel()
        vm.loadTtsModels()

        vm.selectTtsModelVoice("en", "en_GB-alan-medium")

        assertEquals(listOf("en_GB-alan-medium"), selectVoiceCalls)
        assertEquals(1, downloadCalls)
        val model = vm.currentState.ttsModels.single()
        assertTrue(model.isDownloaded)
        assertEquals("en_GB-alan-medium", model.selectedVoiceId)
        assertTrue(vm.currentState.ttsDownloadProgress.isEmpty())
    }

    @Test
    fun `selectTtsModelVoice when model not downloaded only records choice`() = runTest {
        val vm = createViewModel()
        vm.loadTtsModels()

        vm.selectTtsModelVoice("en", "en_GB-alan-medium")

        assertEquals(listOf("en_GB-alan-medium"), selectVoiceCalls)
        assertEquals(0, downloadCalls)
        assertEquals("en_GB-alan-medium", vm.currentState.ttsModels.single().selectedVoiceId)
    }

    @Test
    fun `selectTtsModelVoice when voice already selected does nothing`() = runTest {
        modelDownloaded = true
        val vm = createViewModel()
        vm.loadTtsModels()

        vm.selectTtsModelVoice("en", "en_US-kristin-medium")

        assertTrue(selectVoiceCalls.isEmpty())
        assertEquals(0, downloadCalls)
    }

    @Test
    fun `downloadTtsModel when download fails clears progress and keeps model not downloaded`() = runTest {
        downloadShouldFail = true
        val vm = createViewModel()
        vm.loadTtsModels()

        vm.downloadTtsModel("en")

        assertTrue(vm.currentState.ttsDownloadProgress.isEmpty())
        assertEquals(false, vm.currentState.ttsModels.single().isDownloaded)
    }

    @Test
    fun `previewTtsVoice when model downloaded speaks sample and marks language while playing`() = runTest {
        modelDownloaded = true
        speakGate = CompletableDeferred()
        val vm = createViewModel()
        vm.loadTtsModels()

        vm.previewTtsVoice("en")

        assertEquals(listOf(SAMPLE), spokenTexts)
        assertEquals("en", vm.currentState.ttsPreviewLanguage)

        speakGate?.complete(Unit)
        assertNull(vm.currentState.ttsPreviewLanguage)
    }

    @Test
    fun `previewTtsVoice when model not downloaded does not speak`() = runTest {
        val vm = createViewModel()
        vm.loadTtsModels()

        vm.previewTtsVoice("en")

        assertTrue(spokenTexts.isEmpty())
        assertNull(vm.currentState.ttsPreviewLanguage)
    }

    @Test
    fun `stopTtsPreview stops playback and clears preview state`() = runTest {
        modelDownloaded = true
        speakGate = CompletableDeferred()
        val vm = createViewModel()
        vm.loadTtsModels()
        vm.previewTtsVoice("en")
        val stopsBefore = stopCalls

        vm.stopTtsPreview()

        assertNull(vm.currentState.ttsPreviewLanguage)
        assertTrue(stopCalls > stopsBefore)
    }

    @Test
    fun `setTtsExpressiveness persists value clamped to range`() = runTest {
        val vm = createViewModel()

        vm.setTtsExpressiveness(5f)

        assertEquals(TtsSettings.MAX_EXPRESSIVENESS, lastSetExpressiveness)
    }

    private companion object {
        const val SAMPLE = "Hello! This is how I will read your words."
    }
}
