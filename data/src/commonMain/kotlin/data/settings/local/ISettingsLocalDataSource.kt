package data.settings.local

import data.core.database.SettingsEntityData
import kotlinx.coroutines.flow.Flow

interface ISettingsLocalDataSource {
    fun observeSettings(): Flow<SettingsEntityData?>
    suspend fun getSettings(): SettingsEntityData?
    suspend fun saveSettings(data: SettingsEntityData)
    suspend fun clearSettings()
}
