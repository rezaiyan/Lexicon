package presentation.feature.settings

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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import platform.IAppVersionProvider
import presentation.base.BaseViewModel
import presentation.feature.settings.model.SettingsEffect
import presentation.feature.settings.model.SettingsEvent
import presentation.model.DialogState
import presentation.model.SettingsScreenState
import domain.settings.model.ThemeMode

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

    private val intents = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 64)

    private val systemNotificationsEnabled: StateFlow<Boolean> =
        notificationPermissionMonitor.systemNotificationsEnabled
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true
            )

    init {
        initializeNotificationState()
        observeIntents()
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

    private fun observeIntents() {
        viewModelScope.launch {
            intents.collect { intent -> handleIntent(intent) }
        }
    }

    private suspend fun handleIntent(intent: SettingsEvent) {
        when (intent) {
            is SettingsEvent.SetLanguage -> {
                setLanguageUseCase(intent.language)
                analyticsTracker.logLanguageChanged(language = intent.language.name)
            }
            is SettingsEvent.SetThemeMode -> {
                setThemeModeUseCase(intent.mode)
                analyticsTracker.logThemeChanged(
                    themeMode = intent.mode.displayName,
                    isDark = intent.mode == ThemeMode.DARK
                )
            }
            is SettingsEvent.SetNotificationsEnabled -> {
                setNotificationsEnabledUseCase(intent.enabled)
                if (intent.enabled && !systemNotificationsEnabled.value) {
                    updateState { copy(dialog = DialogState.NotificationPermission) }
                }
            }
            SettingsEvent.RequestNotificationPermission -> {
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
            SettingsEvent.RefreshNotificationPermissionStatus -> {
                notificationPermissionMonitor.refresh()
            }
            is SettingsEvent.ShowDialog -> {
                updateState { copy(dialog = intent.dialogState) }
            }
            SettingsEvent.DismissDialog -> {
                updateState { copy(dialog = DialogState.None) }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        viewModelScope.launch { intents.emit(event) }
    }
}
