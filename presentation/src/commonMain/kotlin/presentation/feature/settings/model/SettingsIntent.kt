package presentation.feature.settings.model

import presentation.model.DialogState
import domain.settings.model.ThemeMode
import utils.Language

sealed class SettingsIntent {
    data class SetLanguage(val language: Language) : SettingsIntent()
    data class SetThemeMode(val mode: ThemeMode) : SettingsIntent()
    data class SetNotificationsEnabled(val enabled: Boolean) : SettingsIntent()
    data object RequestNotificationPermission : SettingsIntent()
    data object RefreshNotificationPermissionStatus : SettingsIntent()
    data class SetReviewSettings(val successesToAdvance: Int, val forgotPenalty: Int) : SettingsIntent()
    data class ShowDialog(val dialogState: DialogState) : SettingsIntent()
    data object DismissDialog : SettingsIntent()
}