package feature.settings

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import domain.auth.repository.IAuthRepository
import domain.notifications.repository.INotificationRepository
import domain.notifications.usecase.OpenNotificationSettingsUseCase
import domain.notifications.usecase.RequestNotificationPermissionUseCase
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.SetLanguageUseCase
import domain.settings.usecase.SetNotificationsEnabledUseCase
import domain.settings.usecase.SetThemeModeUseCase
import domain.tts.model.TtsModelInfo
import domain.tts.usecase.DeleteTtsModelUseCase
import domain.tts.usecase.GetTtsModelsInfoUseCase
import core.common.getOrDefault
import core.common.fold
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import platform.IAppVersionProvider
import core.base.BaseViewModel
import feature.settings.model.SettingsEffect
import feature.settings.model.SettingsScreenState
import domain.settings.model.ThemeMode
import utils.Language

data class SettingsState(
    val screen: SettingsScreenState = SettingsScreenState(),
    val ttsModels: List<TtsModelInfo> = emptyList(),
    val ttsModelsLoading: Boolean = false,
    val ttsTotalSizeBytes: Long = 0L,
    val ttsDownloadedCount: Int = 0,
)

@Suppress("LongParameterList")
class SettingsViewModel(
    private val notificationRepository: INotificationRepository,
    private val setLanguageUseCase: SetLanguageUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    private val requestNotificationPermissionUseCase: RequestNotificationPermissionUseCase,
    private val openNotificationSettingsUseCase: OpenNotificationSettingsUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val notificationPermissionMonitor: NotificationPermissionMonitor,
    private val getTtsModelsInfoUseCase: GetTtsModelsInfoUseCase,
    private val deleteTtsModelUseCase: DeleteTtsModelUseCase,
    settingsRepository: ISettingsRepository,
    authRepository: IAuthRepository,
    appVersionProvider: IAppVersionProvider,
) : BaseViewModel<SettingsState, SettingsEffect>() {

    override fun initialState() = SettingsState()

    private val systemNotificationsEnabled: StateFlow<Boolean> =
        notificationPermissionMonitor.systemNotificationsEnabled
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true
            )

    init {
        initializeNotificationState()
        observeSettingsState(settingsRepository, authRepository, appVersionProvider)
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
                featureAccessFlow = authRepository.getFeatureAccessAsFlow()
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

    fun requestNotificationPermission() {
        viewModelScope.launch {
            val granted = requestNotificationPermissionUseCase().getOrDefault(false)
            emitEffect(SettingsEffect.NotificationPermissionGranted(granted))
            if (granted) {
                setNotificationsEnabledUseCase(true)
            } else {
                emitEffect(SettingsEffect.OpenSystemNotificationSettings)
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

    fun loadTtsModels() {
        viewModelScope.launch {
            updateState { copy(ttsModelsLoading = true) }
            getTtsModelsInfoUseCase().fold(
                onSuccess = { models ->
                    updateState {
                        copy(
                            ttsModels = models,
                            ttsModelsLoading = false,
                            ttsTotalSizeBytes = models.filter { it.isDownloaded }.sumOf { it.sizeBytes },
                            ttsDownloadedCount = models.count { it.isDownloaded },
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
                onSuccess = { loadTtsModels() },
                onFailure = { /* silent failure — model list will reflect current state on next load */ }
            )
        }
    }
}
