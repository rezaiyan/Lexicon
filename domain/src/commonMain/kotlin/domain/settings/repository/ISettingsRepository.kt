package domain.settings.repository

import core.common.Try
import utils.Language
import domain.settings.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Domain layer repository interface for settings
 */
interface ISettingsRepository {
    fun getLanguage(): Flow<Language>
    suspend fun setLanguage(language: Language): Try<Unit>
    fun getThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode): Try<Unit>
    suspend fun clearSettings(): Try<Unit>

    // Notification settings
    fun getNotificationsEnabled(): Flow<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean): Try<Unit>
    fun getReviewRemindersEnabled(): Flow<Boolean>
    suspend fun setReviewRemindersEnabled(enabled: Boolean): Try<Unit>
    fun getMotivationalMessagesEnabled(): Flow<Boolean>
    suspend fun setMotivationalMessagesEnabled(enabled: Boolean): Try<Unit>
    suspend fun getDailyReminderTime(): Try<String>
    suspend fun setDailyReminderTime(time: String): Try<Unit>
    suspend fun getMinimumDueCards(): Try<Int>
    suspend fun setMinimumDueCards(count: Int): Try<Unit>
}

