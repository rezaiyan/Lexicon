package feature.settings.model

sealed class SettingsEffect {
    data class NotificationPermissionGranted(val granted: Boolean) : SettingsEffect()
    data object OpenSystemNotificationSettings : SettingsEffect()
}
