package feature.settings.model

import domain.settings.model.ThemeMode

data class SettingsScreenState(
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val notificationsEnabled: Boolean = true,
    val systemNotificationsEnabled: Boolean = true,
    val reviewRemindersEnabled: Boolean = true,
    val appVersion: String = "Loading.."
)
