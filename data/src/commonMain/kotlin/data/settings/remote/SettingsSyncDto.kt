package data.settings.remote

import kotlinx.serialization.Serializable

@Serializable
data class SettingsSyncDto(
    val languageCode: String,
    val themeMode: String,
    val notificationsEnabled: Boolean,
    val dailyReminderTime: String,
    val reviewRemindersEnabled: Boolean,
)
