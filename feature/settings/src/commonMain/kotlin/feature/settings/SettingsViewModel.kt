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
import core.common.getOrDefault
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import platform.IAppVersionProvider
import core.base.BaseViewModel
import feature.settings.model.DialogState
import feature.settings.model.SettingsEffect
import feature.settings.model.SettingsScreenState
import domain.settings.model.ThemeMode
import utils.Language

data class SettingsState(
    val screen: SettingsScreenState = SettingsScreenState(),
    val dialog: DialogState = DialogState.None,
)

class SettingsViewModel(
    private val notificationRepository: INotificationRepository,
    private val setLanguageUseCase: SetLanguageUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    private val requestNotificationPermissionUseCase: RequestNotificationPermissionUseCase,
    private val openNotificationSettingsUseCase: OpenNotificationSettingsUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val notificationPermissionMonitor: NotificationPermissionMonitor,
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
            val systemEnabled = notificationRepository.areNotificationsEnabled()
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
            if (enabled && !systemNotificationsEnabled.value) {
                updateState { copy(dialog = DialogState.NotificationPermission) }
            }
        }
    }

    fun requestNotificationPermission() {
        viewModelScope.launch {
            val granted = requestNotificationPermissionUseCase().getOrDefault(false)
            emitEffect(SettingsEffect.NotificationPermissionGranted(granted))
            updateState { copy(dialog = DialogState.None) }
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

    fun showDialog(dialogState: DialogState) {
        updateState { copy(dialog = dialogState) }
    }

    fun dismissDialog() {
        updateState { copy(dialog = DialogState.None) }
    }
}
