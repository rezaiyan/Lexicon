package presentation.feature.settings.model

import presentation.model.DialogState

sealed class SettingsEffect {
    data class ShowDialog(val dialogState: DialogState) : SettingsEffect()
    data object DismissDialog : SettingsEffect()
    data class NotificationPermissionGranted(val granted: Boolean) : SettingsEffect()
    data object OpenSystemNotificationSettings : SettingsEffect()
}
