package feature.settings

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import domain.auth.repository.IAuthRepository
import domain.notifications.repository.INotificationRepository
import domain.notifications.usecase.OpenNotificationSettingsUseCase
import domain.notifications.usecase.RequestNotificationPermissionUseCase
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.GetDailyGoalWordsUseCase
import domain.settings.usecase.SetDailyGoalWordsUseCase
import domain.settings.usecase.SetLanguageUseCase
import domain.settings.usecase.SetNotificationsEnabledUseCase
import domain.settings.usecase.SetReviewRemindersEnabledUseCase
import domain.settings.usecase.SetThemeModeUseCase
import domain.settings.usecase.SetTtsVoiceUseCase
import domain.settings.usecase.SetTtsSpeechRateUseCase
import domain.tts.model.TtsModelInfo
import domain.tts.model.TtsSettings
import domain.tts.usecase.DeleteTtsModelUseCase
import domain.tts.usecase.DownloadTtsModelUseCase
import domain.tts.usecase.GetTtsModelsInfoUseCase
import core.common.getOrDefault
import core.common.fold
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import platform.IAppVersionProvider
import core.base.BaseViewModel
import feature.settings.model.SettingsScreenState
import domain.settings.model.ThemeMode
import utils.Language

data class SettingsState(
    val screen: SettingsScreenState = SettingsScreenState(),
    val ttsModels: List<TtsModelInfo> = emptyList(),
    val ttsModelsLoading: Boolean = false,
    val ttsTotalSizeBytes: Long = 0L,
    val ttsSettings: TtsSettings = TtsSettings(),
    val ttsDownloadProgress: Map<String, Float> = emptyMap(),
    val dailyGoalWords: Int = 10,
)

@Suppress("LongParameterList")
class SettingsViewModel(
    private val notificationRepository: INotificationRepository,
    private val setLanguageUseCase: SetLanguageUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    private val setReviewRemindersEnabledUseCase: SetReviewRemindersEnabledUseCase,
    private val requestNotificationPermissionUseCase: RequestNotificationPermissionUseCase,
    private val openNotificationSettingsUseCase: OpenNotificationSettingsUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val notificationPermissionMonitor: NotificationPermissionMonitor,
    private val getTtsModelsInfoUseCase: GetTtsModelsInfoUseCase,
    private val deleteTtsModelUseCase: DeleteTtsModelUseCase,
    private val downloadTtsModelUseCase: DownloadTtsModelUseCase,
    private val setTtsSpeechRateUseCase: SetTtsSpeechRateUseCase,
    private val setTtsVoiceUseCase: SetTtsVoiceUseCase,
    private val getDailyGoalWordsUseCase: GetDailyGoalWordsUseCase,
    private val setDailyGoalWordsUseCase: SetDailyGoalWordsUseCase,
    settingsRepository: ISettingsRepository,
    authRepository: IAuthRepository,
    appVersionProvider: IAppVersionProvider,
) : BaseViewModel<SettingsState, Nothing>() {

    override fun initialState() = SettingsState()

    private val systemNotificationsEnabled =
        notificationPermissionMonitor.systemNotificationsEnabled
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true
            )

    init {
        initializeNotificationState()
        observeSettingsState(settingsRepository, authRepository, appVersionProvider)
        observeTtsSettings(settingsRepository)
        loadDailyGoal()
    }

    private fun observeSettingsState(
        settingsRepository: ISettingsRepository,
        authRepository: IAuthRepository,
        appVersionProvider: IAppVersionProvider,
    ) {
        viewModelScope.launch {
            SettingsStateBuilder.buildStateFlow(
                currentLanguage = settingsRepository.getLanguage(),
                themeMode = settingsRepository.getThemeMode(),
                notificationsEnabled = settingsRepository.getNotificationsEnabled(),
                systemNotificationsEnabled = systemNotificationsEnabled,
                appVersion = flowOf(appVersionProvider.getVersion()),
                featureAccessFlow = authRepository.getFeatureAccessAsFlow(),
                reviewRemindersEnabled = settingsRepository.getReviewRemindersEnabled()
            ).catch { e ->
                analyticsTracker.logNonFatalError(
                    message = "Settings state build failed",
                    additionalInfo = mapOf("error" to (e.message ?: "unknown"))
                )
            }.collect { screenState ->
                updateState { copy(screen = screenState) }
            }
        }
    }

    private fun observeTtsSettings(settingsRepository: ISettingsRepository) {
        settingsRepository.getTtsSettings()
            .onEach { settings -> updateState { copy(ttsSettings = settings) } }
            .launchIn(viewModelScope)
    }

    private fun loadDailyGoal() {
        viewModelScope.launch {
            getDailyGoalWordsUseCase(Unit).fold(
                onSuccess = { count -> updateState { copy(dailyGoalWords = count) } },
                onFailure = { /* keep default */ }
            )
        }
    }

    private fun initializeNotificationState() {
        viewModelScope.launch {
            val systemEnabled = notificationRepository.areNotificationsEnabled().getOrDefault(true)
            if (!systemEnabled) {
                setNotificationsEnabledUseCase(false)
            }
            notificationPermissionMonitor.refresh()
        }
    }

    fun setLanguage(language: Language) {
        viewModelScope.launch {
            setLanguageUseCase(language)
            analyticsTracker.logLanguageChanged(language = language.name)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            setThemeModeUseCase(mode)
            analyticsTracker.logThemeChanged(
                themeMode = mode.displayName,
                isDark = mode == ThemeMode.DARK
            )
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            setNotificationsEnabledUseCase(enabled)
        }
    }

    fun setReviewRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            setReviewRemindersEnabledUseCase(enabled)
        }
    }

    fun requestNotificationPermission() {
        viewModelScope.launch {
            val granted = requestNotificationPermissionUseCase().getOrDefault(false)
            if (granted) {
                setNotificationsEnabledUseCase(true)
            } else {
                openNotificationSettingsUseCase()
            }
            notificationPermissionMonitor.refresh()
        }
    }

    fun refreshNotificationPermissionStatus() {
        viewModelScope.launch {
            notificationPermissionMonitor.refresh()
        }
    }

    fun loadTtsModels(silently: Boolean = false) {
        viewModelScope.launch {
            if (!silently) updateState { copy(ttsModelsLoading = true) }
            getTtsModelsInfoUseCase().fold(
                onSuccess = { models ->
                    updateState {
                        copy(
                            ttsModels = models,
                            ttsModelsLoading = false,
                            ttsTotalSizeBytes = models.filter { it.isDownloaded }.sumOf { it.sizeBytes },
                        )
                    }
                },
                onFailure = {
                    updateState { copy(ttsModelsLoading = false) }
                }
            )
        }
    }

    fun deleteTtsModel(languageCode: String) {
        viewModelScope.launch {
            deleteTtsModelUseCase(languageCode).fold(
                onSuccess = { loadTtsModels(silently = true) },
                onFailure = { /* silent failure — model list will reflect current state on next load */ }
            )
        }
    }

    fun downloadTtsModel(languageCode: String) {
        viewModelScope.launch {
            downloadTtsModelUseCase(languageCode)
                .collect { progress ->
                    updateState { copy(ttsDownloadProgress = ttsDownloadProgress + (languageCode to progress)) }
                }
            updateState { copy(ttsDownloadProgress = ttsDownloadProgress - languageCode) }
            loadTtsModels(silently = true)
        }
    }

    fun setTtsSpeechRate(rate: Float) {
        viewModelScope.launch {
            setTtsSpeechRateUseCase(rate)
        }
    }

    fun setDailyGoalWords(count: Int) {
        viewModelScope.launch {
            setDailyGoalWordsUseCase(count).fold(
                onSuccess = { updateState { copy(dailyGoalWords = count) } },
                onFailure = { /* silent — state remains unchanged */ }
            )
        }
    }

    fun setTtsVoice(languageCode: String, speakerId: Int) {
        viewModelScope.launch {
            setTtsVoiceUseCase(SetTtsVoiceUseCase.Params(languageCode, speakerId))
            loadTtsModels(silently = true)
        }
    }
}
