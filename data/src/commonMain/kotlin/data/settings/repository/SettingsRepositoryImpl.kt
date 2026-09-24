package data.settings.repository

import core.common.Try
import data.core.database.SettingsEntityData
import data.settings.local.ISettingsLocalDataSource
import data.settings.remote.ISettingsRemoteDataSource
import data.settings.remote.SettingsSyncDto
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.tts.model.TtsSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import utils.Language

class SettingsRepositoryImpl(
    private val localDataSource: ISettingsLocalDataSource,
    private val remoteDataSource: ISettingsRemoteDataSource,
    private val scope: CoroutineScope,
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

    override suspend fun setNotificationsEnabled(enabled: Boolean): Try<Unit> {
        return Try {
            val current = localDataSource.getSettings() ?: SettingsEntityData()
            val updated = current.copy(notificationsEnabled = enabled)
            localDataSource.saveSettings(updated)
            scope.launch { remoteDataSource.syncSettings(updated.toSyncDto()) }
        }
    }

    override fun getReviewRemindersEnabled(): Flow<Boolean> {
        return localDataSource.observeSettings().map { it?.reviewReminders ?: true }
    }

    override suspend fun setReviewRemindersEnabled(enabled: Boolean): Try<Unit> {
        return try {
            val current = localDataSource.getSettings() ?: SettingsEntityData()
            val updated = current.copy(reviewReminders = enabled)
            localDataSource.saveSettings(updated)
            scope.launch { remoteDataSource.syncSettings(updated.toSyncDto()) }
            Try.Success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Throwable) {
            Try.Failure(e)
        }
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

    override fun getSkipTagSelector(): Flow<Boolean> =
        localDataSource.observeSettings().map { it?.skipTagSelector ?: false }

    override suspend fun setSkipTagSelector(skip: Boolean): Try<Unit> = Try {
        val current = localDataSource.getSettings() ?: SettingsEntityData()
        localDataSource.saveSettings(current.copy(skipTagSelector = skip))
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

    override suspend fun getDailyGoalWords(): Try<Int> = Try {
        localDataSource.getSettings()?.dailyGoalWords ?: 10
    }

    override suspend fun setDailyGoalWords(count: Int): Try<Unit> = Try {
        val current = localDataSource.getSettings() ?: SettingsEntityData()
        localDataSource.saveSettings(current.copy(dailyGoalWords = count))
    }

    override suspend fun clearSettings(): Try<Unit> = Try {
        localDataSource.clearSettings()
    }
}

private fun SettingsEntityData.toSyncDto() = SettingsSyncDto(
    languageCode = languageCode,
    themeMode = themeMode,
    notificationsEnabled = notificationsEnabled,
    dailyReminderTime = dailyReminderTime,
    reviewRemindersEnabled = reviewReminders,
)
