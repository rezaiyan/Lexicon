package data.settings.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import data.core.database.LexiconQueries
import data.core.database.SettingsEntity
import data.core.database.SettingsEntityData
import domain.settings.repository.ISettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import domain.settings.model.ThemeMode
import utils.Language

/**
 * Local-only settings repository
 * Settings are stored purely on the client side with no remote synchronization
 */
class SettingsRepositoryImpl(
    private val queries: LexiconQueries
) : ISettingsRepository {

    private fun getSettingsFlow(): Flow<SettingsEntity?> =
        queries.getSettings().asFlow().mapToOneOrNull(Dispatchers.Default)

    private fun SettingsEntity.toData(): SettingsEntityData = SettingsEntityData(
        id = id.toInt(),
        languageCode = languageCode,
        themeMode = themeMode,
        notificationsEnabled = notificationsEnabled != 0L,
        reviewReminders = reviewReminders != 0L,
        motivationalMessages = motivationalMessages != 0L,
        dailyReminderTime = dailyReminderTime,
        minimumDueCards = minimumDueCards.toInt()
    )

    private suspend fun insertSettingsData(data: SettingsEntityData) {
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

    override fun getLanguage(): Flow<Language> {
        return getSettingsFlow()
            .map { settings -> Language.fromCode(settings?.languageCode ?: "en") }
    }

    override suspend fun setLanguage(language: Language) {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        val updated = current.copy(languageCode = language.code)
        insertSettingsData(updated)
    }

    override fun getThemeMode(): Flow<ThemeMode> {
        return getSettingsFlow()
            .map { settings -> ThemeMode.fromString(settings?.themeMode ?: ThemeMode.AUTO.name) }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        val updated = current.copy(themeMode = mode.name)
        insertSettingsData(updated)
    }

    override fun getNotificationsEnabled(): Flow<Boolean> {
        return getSettingsFlow().map { it?.notificationsEnabled != 0L }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        val updated = current.copy(notificationsEnabled = enabled)
        insertSettingsData(updated)
    }

    override fun getReviewRemindersEnabled(): Flow<Boolean> {
        return getSettingsFlow().map { it?.reviewReminders != 0L }
    }

    override suspend fun setReviewRemindersEnabled(enabled: Boolean) {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        val updated = current.copy(reviewReminders = enabled)
        insertSettingsData(updated)
    }

    override fun getMotivationalMessagesEnabled(): Flow<Boolean> {
        return getSettingsFlow().map { it?.motivationalMessages != 0L }
    }

    override suspend fun setMotivationalMessagesEnabled(enabled: Boolean) {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        val updated = current.copy(motivationalMessages = enabled)
        insertSettingsData(updated)
    }

    override suspend fun getDailyReminderTime(): String {
        return queries.getSettings().awaitAsOneOrNull()?.dailyReminderTime ?: "18:00"
    }

    override suspend fun setDailyReminderTime(time: String) {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        val updated = current.copy(dailyReminderTime = time)
        insertSettingsData(updated)
    }

    override suspend fun getMinimumDueCards(): Int {
        return queries.getSettings().awaitAsOneOrNull()?.minimumDueCards?.toInt() ?: 5
    }

    override suspend fun setMinimumDueCards(count: Int) {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        val updated = current.copy(minimumDueCards = count)
        insertSettingsData(updated)
    }

    override suspend fun clearSettings() {
        queries.clearSettings()
    }
}
