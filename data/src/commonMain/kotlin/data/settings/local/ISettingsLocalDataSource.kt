package data.settings.local

import data.core.database.SettingsEntityData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface ISettingsLocalDataSource {
    fun observeSettings(): Flow<SettingsEntityData?>
    suspend fun getSettings(): SettingsEntityData?
    suspend fun saveSettings(data: SettingsEntityData)
    suspend fun clearSettings()

    suspend fun getWordSyncTimestamp(): Long = 0L
    suspend fun setWordSyncTimestamp(timestamp: Long) {}

    // Per-language voice preferences — default implementations keep existing fakes compiling
    fun observeVoicePreferences(): Flow<Map<String, Int>> = flowOf(emptyMap())
    suspend fun setVoiceForLanguage(languageCode: String, speakerId: Int) {}
    fun getNumSpeakersForLanguage(languageCode: String): Flow<Int> = flowOf(1)
    suspend fun cacheNumSpeakersForLanguage(languageCode: String, numSpeakers: Int) {}
}
