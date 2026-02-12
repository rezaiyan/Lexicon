package presentation.feature.settings.model

import presentation.model.DialogState

sealed class SettingsEvent {
    data class ShowDialog(val dialogState: DialogState) : SettingsEvent()
    data object DismissDialog : SettingsEvent()
    data class NotificationPermissionGranted(val granted: Boolean) : SettingsEvent()
    data object OpenSystemNotificationSettings : SettingsEvent()
}
