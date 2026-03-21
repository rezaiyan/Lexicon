package data.settings.repository

import core.common.Try
import data.core.database.SettingsEntityData
import data.settings.local.ISettingsLocalDataSource
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.tts.model.TtsSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import utils.Language

class SettingsRepositoryImpl(
    private val localDataSource: ISettingsLocalDataSource
) : ISettingsRepository {

    override fun getLanguage(): Flow<Language> {
        return localDataSource.observeSettings()
            .map { settings -> Language.fromCode(settings?.languageCode ?: "en") }
    }

    override suspend fun setLanguage(language: Language): Try<Unit> = Try {
        val current = localDataSource.getSettings() ?: SettingsEntityData()
        val updated = current.copy(languageCode = language.code)
        localDataSource.saveSettings(updated)
    }

    override fun getThemeMode(): Flow<ThemeMode> {
        return localDataSource.observeSettings()
            .map { settings -> ThemeMode.fromString(settings?.themeMode ?: ThemeMode.AUTO.name) }
    }

    override suspend fun setThemeMode(mode: ThemeMode): Try<Unit> = Try {
        val current = localDataSource.getSettings() ?: SettingsEntityData()
        val updated = current.copy(themeMode = mode.name)
        localDataSource.saveSettings(updated)
    }

    override fun getNotificationsEnabled(): Flow<Boolean> {
        return localDataSource.observeSettings().map { it?.notificationsEnabled ?: true }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean): Try<Unit> = Try {
        val current = localDataSource.getSettings() ?: SettingsEntityData()
        val updated = current.copy(notificationsEnabled = enabled)
        localDataSource.saveSettings(updated)
    }

    override fun getReviewRemindersEnabled(): Flow<Boolean> {
        return localDataSource.observeSettings().map { it?.reviewReminders ?: true }
    }

    override suspend fun setReviewRemindersEnabled(enabled: Boolean): Try<Unit> = Try {
        val current = localDataSource.getSettings() ?: SettingsEntityData()
        val updated = current.copy(reviewReminders = enabled)
        localDataSource.saveSettings(updated)
    }

    override fun getMotivationalMessagesEnabled(): Flow<Boolean> {
        return localDataSource.observeSettings().map { it?.motivationalMessages ?: true }
    }

    override suspend fun setMotivationalMessagesEnabled(enabled: Boolean): Try<Unit> = Try {
        val current = localDataSource.getSettings() ?: SettingsEntityData()
        val updated = current.copy(motivationalMessages = enabled)
        localDataSource.saveSettings(updated)
    }

    override suspend fun getDailyReminderTime(): Try<String> = Try {
        localDataSource.getSettings()?.dailyReminderTime ?: "18:00"
    }

    override suspend fun setDailyReminderTime(time: String): Try<Unit> = Try {
        val current = localDataSource.getSettings() ?: SettingsEntityData()
        val updated = current.copy(dailyReminderTime = time)
        localDataSource.saveSettings(updated)
    }

    override suspend fun getMinimumDueCards(): Try<Int> = Try {
        localDataSource.getSettings()?.minimumDueCards ?: 5
    }

    override suspend fun setMinimumDueCards(count: Int): Try<Unit> = Try {
        val current = localDataSource.getSettings() ?: SettingsEntityData()
        val updated = current.copy(minimumDueCards = count)
        localDataSource.saveSettings(updated)
    }

    override fun getTtsSettings(): Flow<TtsSettings> {
        return localDataSource.observeSettings().map { settings ->
            TtsSettings(
                speechRate = settings?.ttsSpeed ?: TtsSettings.DEFAULT_SPEECH_RATE,
            )
        }
    }

    override suspend fun setTtsSpeechRate(rate: Float): Try<Unit> = Try {
        val current = localDataSource.getSettings() ?: SettingsEntityData()
        localDataSource.saveSettings(current.copy(ttsSpeed = rate))
    }

    override fun getTtsVoiceForLanguage(languageCode: String): Flow<Int> =
        localDataSource.observeVoicePreferences().map { prefs ->
            prefs[languageCode] ?: TtsSettings.DEFAULT_SPEAKER_ID
        }

    override suspend fun setTtsVoiceForLanguage(languageCode: String, speakerId: Int): Try<Unit> = Try {
        localDataSource.setVoiceForLanguage(languageCode, speakerId)
    }

    override fun getNumSpeakersForLanguage(languageCode: String): Flow<Int> =
        localDataSource.getNumSpeakersForLanguage(languageCode)

    override suspend fun cacheNumSpeakersForLanguage(languageCode: String, numSpeakers: Int): Try<Unit> = Try {
        localDataSource.cacheNumSpeakersForLanguage(languageCode, numSpeakers)
    }

    override suspend fun clearSettings(): Try<Unit> = Try {
        localDataSource.clearSettings()
    }
}
