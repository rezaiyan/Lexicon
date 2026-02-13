package domain.settings.repository

import utils.Language
import domain.settings.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Domain layer repository interface for settings
 */
interface ISettingsRepository {
    fun getLanguage(): Flow<Language>
    suspend fun setLanguage(language: Language)
    fun getThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun getLastInsightDate(): String?
    suspend fun getCachedInsight(): String?
    suspend fun updateDailyInsight(date: String, insight: String)
    suspend fun getLastInsightDismissedTime(): Long
    suspend fun setLastInsightDismissedTime(timestamp: Long)
    suspend fun clearInsightData()
    suspend fun clearSettings()
    
    // Notification settings
    fun getNotificationsEnabled(): Flow<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean)
    fun getReviewRemindersEnabled(): Flow<Boolean>
    suspend fun setReviewRemindersEnabled(enabled: Boolean)
    fun getMotivationalMessagesEnabled(): Flow<Boolean>
    suspend fun setMotivationalMessagesEnabled(enabled: Boolean)
    suspend fun getDailyReminderTime(): String
    suspend fun setDailyReminderTime(time: String)
    suspend fun getMinimumDueCards(): Int
    suspend fun setMinimumDueCards(count: Int)
}

