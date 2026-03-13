package data.settings.local

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
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
                        minimumDueCards = it.minimumDueCards.toInt()
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
            minimumDueCards = entity.minimumDueCards.toInt()
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
            minimumDueCards = data.minimumDueCards.toLong()
        )
    }

    override suspend fun clearSettings() {
        queries.clearSettings()
    }
}
