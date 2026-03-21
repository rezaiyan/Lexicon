package domain.settings.repository

import core.common.Try
import domain.settings.model.ThemeMode
import domain.tts.model.TtsSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import utils.Language

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

    // TTS settings — default implementations keep existing fakes compiling
    fun getTtsSettings(): Flow<TtsSettings> = flowOf(TtsSettings())
    suspend fun setTtsSpeechRate(rate: Float): Try<Unit> = Try.success(Unit)

    // Per-language voice preference
    fun getTtsVoiceForLanguage(languageCode: String): Flow<Int> = flowOf(TtsSettings.DEFAULT_SPEAKER_ID)
    suspend fun setTtsVoiceForLanguage(languageCode: String, speakerId: Int): Try<Unit> = Try.success(Unit)

    // Cached speaker count per language (persisted after first model load)
    fun getNumSpeakersForLanguage(languageCode: String): Flow<Int> = flowOf(1)
    suspend fun cacheNumSpeakersForLanguage(languageCode: String, numSpeakers: Int): Try<Unit> = Try.success(Unit)
}

