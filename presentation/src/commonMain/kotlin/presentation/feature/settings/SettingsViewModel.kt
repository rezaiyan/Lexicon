package presentation.feature.settings

import analytics.IAnalyticsTracker
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.auth.repository.IAuthRepository
import domain.notifications.repository.INotificationRepository
import domain.notifications.usecase.OpenNotificationSettingsUseCase
import domain.notifications.usecase.RequestNotificationPermissionUseCase
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.SetLanguageUseCase
import domain.settings.usecase.SetNotificationsEnabledUseCase
import domain.settings.usecase.SetThemeModeUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import platform.IAppVersionProvider
import presentation.feature.settings.model.SettingsEffect
import presentation.feature.settings.model.SettingsEvent
import presentation.model.DialogState
import presentation.model.SettingsScreenState
import domain.settings.model.ThemeMode

class SettingsViewModel(
    private val notificationRepository: INotificationRepository, // Keep for areNotificationsEnabled()
    private val setLanguageUseCase: SetLanguageUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    private val requestNotificationPermissionUseCase: RequestNotificationPermissionUseCase,
    private val openNotificationSettingsUseCase: OpenNotificationSettingsUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val notificationPermissionMonitor: NotificationPermissionMonitor,
    settingsRepository: ISettingsRepository, // Keep for read-only state flows
    authRepository: IAuthRepository,
    appVersionProvider: IAppVersionProvider,
) : ViewModel() {

    private val _dialogState = MutableStateFlow<DialogState>(DialogState.None)
    val dialogState: StateFlow<DialogState> = _dialogState

    private val intents = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 64)
    private val _events = MutableSharedFlow<SettingsEffect>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    private val systemNotificationsEnabled: StateFlow<Boolean> =
        notificationPermissionMonitor.systemNotificationsEnabled
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true
            )

    val settingsScreenState: StateFlow<SettingsScreenState> = SettingsStateBuilder.buildStateFlow(
        currentLanguage = settingsRepository.getLanguage(),
        themeMode = settingsRepository.getThemeMode(),
        notificationsEnabled = settingsRepository.getNotificationsEnabled(),
        systemNotificationsEnabled = systemNotificationsEnabled,
        appVersion = flowOf(appVersionProvider.getVersion()),
        featureAccessFlow = authRepository.getFeatureAccessAsFlow()
    ).catch {

    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsScreenState()
        )

    init {
        initializeNotificationState()
        observeIntents()
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
                    _dialogState.value = DialogState.NotificationPermission
                }
            }
            SettingsEvent.RequestNotificationPermission -> {
                val granted = requestNotificationPermissionUseCase()
                _events.emit(SettingsEffect.NotificationPermissionGranted(granted))
                _dialogState.value = DialogState.None
                if (granted) {
                    setNotificationsEnabledUseCase(true)
                } else {
                    _events.emit(SettingsEffect.OpenSystemNotificationSettings)
                    openNotificationSettingsUseCase()
                }
                notificationPermissionMonitor.refresh()
            }
            SettingsEvent.RefreshNotificationPermissionStatus -> {
                notificationPermissionMonitor.refresh()
            }
            is SettingsEvent.ShowDialog -> {
                _dialogState.value = intent.dialogState
            }
            SettingsEvent.DismissDialog -> {
                _dialogState.value = DialogState.None
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        viewModelScope.launch { intents.emit(event) }
    }
}