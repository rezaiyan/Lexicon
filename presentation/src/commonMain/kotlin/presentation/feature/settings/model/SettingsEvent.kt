package presentation.feature.settings.model

import presentation.model.DialogState
import domain.settings.model.ThemeMode
import utils.Language

sealed class SettingsEvent {
    data class SetLanguage(val language: Language) : SettingsEvent()
    data class SetThemeMode(val mode: ThemeMode) : SettingsEvent()
    data class SetNotificationsEnabled(val enabled: Boolean) : SettingsEvent()
    data object RequestNotificationPermission : SettingsEvent()
    data object RefreshNotificationPermissionStatus : SettingsEvent()
    data class SetReviewSettings(val successesToAdvance: Int, val forgotPenalty: Int) : SettingsEvent()
    data class ShowDialog(val dialogState: DialogState) : SettingsEvent()
    data object DismissDialog : SettingsEvent()
}