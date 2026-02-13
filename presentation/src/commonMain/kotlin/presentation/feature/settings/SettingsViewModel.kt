package presentation.feature.settings

import analytics.IAnalyticsTracker
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.auth.repository.IAuthRepository
import domain.notifications.repository.INotificationRepository
import domain.settings.repository.ISettingsRepository
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
import utils.Language

class SettingsViewModel(
    private val settingsRepository: ISettingsRepository,
    private val notificationRepository: INotificationRepository,
    private val analyticsTracker: IAnalyticsTracker,
    private val authRepository: IAuthRepository,
    private val notificationPermissionMonitor: NotificationPermissionMonitor,
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
        successesToAdvance = settingsRepository.getSuccessesToAdvance(),
        forgotPenalty = settingsRepository.getForgotPenalty(),
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
                settingsRepository.setNotificationsEnabled(false)
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
                settingsRepository.setLanguage(intent.language)
                analyticsTracker.logLanguageChanged(language = intent.language.name)
            }
            is SettingsEvent.SetThemeMode -> {
                settingsRepository.setThemeMode(intent.mode)
                analyticsTracker.logThemeChanged(
                    themeMode = intent.mode.displayName,
                    isDark = intent.mode == ThemeMode.DARK
                )
            }
            is SettingsEvent.SetNotificationsEnabled -> {
                settingsRepository.setNotificationsEnabled(intent.enabled)
                if (intent.enabled && !systemNotificationsEnabled.value) {
                    _dialogState.value = DialogState.NotificationPermission
                }
            }
            SettingsEvent.RequestNotificationPermission -> {
                val granted = notificationRepository.requestNotificationPermission()
                _events.emit(SettingsEffect.NotificationPermissionGranted(granted))
                _dialogState.value = DialogState.None
                if (granted) {
                    settingsRepository.setNotificationsEnabled(true)
                } else {
                    _events.emit(SettingsEffect.OpenSystemNotificationSettings)
                    notificationRepository.openNotificationSettings()
                }
                notificationPermissionMonitor.refresh()
            }
            SettingsEvent.RefreshNotificationPermissionStatus -> {
                notificationPermissionMonitor.refresh()
            }
            is SettingsEvent.SetReviewSettings -> {
                settingsRepository.setSuccessesToAdvance(intent.successesToAdvance)
                settingsRepository.setForgotPenalty(intent.forgotPenalty)
                analyticsTracker.logNonFatalError(
                    "Review settings changed",
                    mapOf(
                        "successesToAdvance" to intent.successesToAdvance.toString(),
                        "forgotPenalty" to intent.forgotPenalty.toString()
                    )
                )
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