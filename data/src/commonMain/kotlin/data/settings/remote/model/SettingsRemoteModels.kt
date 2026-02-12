package data.settings.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class RemoteSettings(
    val languageCode: String,
    val themeMode: String,
    val notificationsEnabled: Boolean,
    val reviewReminders: Boolean,
    val motivationalMessages: Boolean,
    val dailyReminderTime: String,
    val minimumDueCards: Int,
    val successesToAdvance: Int,
    val forgotPenalty: Int
)

