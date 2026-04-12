package data.settings.local

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import data.core.database.LexiconQueries
import data.core.database.SettingsEntityData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsLocalDataSourceImpl(
    private val queries: LexiconQueries
) : ISettingsLocalDataSource {

    override fun observeSettings(): Flow<SettingsEntityData?> =
        queries.getSettings().asFlow().mapToOneOrNull(Dispatchers.Default)
            .map { entity ->
                entity?.let {
                    SettingsEntityData(
                        id = it.id.toInt(),
                        languageCode = it.languageCode,
                        themeMode = it.themeMode,
                        notificationsEnabled = it.notificationsEnabled != 0L,
                        reviewReminders = it.reviewReminders != 0L,
                        motivationalMessages = it.motivationalMessages != 0L,
                        dailyReminderTime = it.dailyReminderTime,
                        minimumDueCards = it.minimumDueCards.toInt(),
                        ttsSpeed = it.ttsSpeed.toFloat(),
                        ttsSpeakerId = it.ttsSpeakerId.toInt(),
                        skipTagSelector = it.skipTagSelector != 0L,
                        dailyGoalWords = it.dailyGoalWords.toInt(),
                    )
                }
            }

    override suspend fun getSettings(): SettingsEntityData? {
        val entity = queries.getSettings().awaitAsOneOrNull() ?: return null
        return SettingsEntityData(
            id = entity.id.toInt(),
            languageCode = entity.languageCode,
            themeMode = entity.themeMode,
            notificationsEnabled = entity.notificationsEnabled != 0L,
            reviewReminders = entity.reviewReminders != 0L,
            motivationalMessages = entity.motivationalMessages != 0L,
            dailyReminderTime = entity.dailyReminderTime,
            minimumDueCards = entity.minimumDueCards.toInt(),
            ttsSpeed = entity.ttsSpeed.toFloat(),
            ttsSpeakerId = entity.ttsSpeakerId.toInt(),
            skipTagSelector = entity.skipTagSelector != 0L,
            dailyGoalWords = entity.dailyGoalWords.toInt(),
        )
    }

    override suspend fun saveSettings(data: SettingsEntityData) {
        queries.insertSettings(
            id = data.id.toLong(),
            languageCode = data.languageCode,
            themeMode = data.themeMode,
            notificationsEnabled = if (data.notificationsEnabled) 1L else 0L,
            reviewReminders = if (data.reviewReminders) 1L else 0L,
            motivationalMessages = if (data.motivationalMessages) 1L else 0L,
            dailyReminderTime = data.dailyReminderTime,
            minimumDueCards = data.minimumDueCards.toLong(),
            ttsSpeed = data.ttsSpeed.toDouble(),
            ttsSpeakerId = data.ttsSpeakerId.toLong(),
            skipTagSelector = if (data.skipTagSelector) 1L else 0L,
            dailyGoalWords = data.dailyGoalWords.toLong(),
        )
    }

    override suspend fun clearSettings() {
        queries.clearSettings()
    }

    override suspend fun getWordSyncTimestamp(): Long =
        queries.getWordSyncTimestamp().awaitAsOneOrNull() ?: 0L

    override suspend fun setWordSyncTimestamp(timestamp: Long) {
        queries.setWordSyncTimestamp(timestamp)
    }

    override fun observeVoicePreferences(): Flow<Map<String, Int>> =
        queries.selectAllVoicePreferences().asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.associate { it.languageCode to it.speakerId.toInt() } }

    override suspend fun setVoiceForLanguage(languageCode: String, speakerId: Int) {
        val existing = queries.selectVoicePreferenceForLanguage(languageCode).awaitAsOneOrNull()
        val currentNumSpeakers = existing?.numSpeakers ?: 1L
        queries.upsertVoicePreference(languageCode, speakerId.toLong(), currentNumSpeakers)
    }

    override fun getNumSpeakersForLanguage(languageCode: String): Flow<Int> =
        queries.selectAllVoicePreferences().asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.find { it.languageCode == languageCode }?.numSpeakers?.toInt() ?: 1 }

    override suspend fun cacheNumSpeakersForLanguage(languageCode: String, numSpeakers: Int) {
        val existing = queries.selectVoicePreferenceForLanguage(languageCode).awaitAsOneOrNull()
        val currentSpeakerId = existing?.speakerId ?: 0L
        queries.upsertVoicePreference(languageCode, currentSpeakerId, numSpeakers.toLong())
    }
}
