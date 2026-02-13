package data.settings.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import data.core.database.LexiconQueries
import data.core.database.SettingsEntity
import data.core.database.SettingsEntityData
import data.settings.remote.SettingsRemoteDataSource
import data.settings.remote.model.RemoteSettings
import domain.settings.repository.ISettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import domain.settings.model.ThemeMode
import utils.Language

class SettingsRepositoryImpl(
    private val queries: LexiconQueries,
    private val settingsRemoteDataSource: SettingsRemoteDataSource
) : ISettingsRepository {

    private fun getSettingsFlow(): Flow<SettingsEntity?> =
        queries.getSettings().asFlow().mapToOneOrNull(Dispatchers.Default)

    private fun SettingsEntity.toData(): SettingsEntityData = SettingsEntityData(
        id = id.toInt(),
        languageCode = languageCode,
        themeMode = themeMode,
        lastInsightDate = lastInsightDate,
        cachedInsight = cachedInsight,
        lastInsightDismissedTime = lastInsightDismissedTime,
        notificationsEnabled = notificationsEnabled != 0L,
        reviewReminders = reviewReminders != 0L,
        motivationalMessages = motivationalMessages != 0L,
        dailyReminderTime = dailyReminderTime,
        minimumDueCards = minimumDueCards.toInt(),
        successesToAdvance = successesToAdvance.toInt(),
        forgotPenalty = forgotPenalty.toInt()
    )

    private suspend fun insertSettingsData(data: SettingsEntityData) {
        queries.insertSettings(
            id = data.id.toLong(),
            languageCode = data.languageCode,
            themeMode = data.themeMode,
            lastInsightDate = data.lastInsightDate,
            cachedInsight = data.cachedInsight,
            lastInsightDismissedTime = data.lastInsightDismissedTime,
            notificationsEnabled = if (data.notificationsEnabled) 1L else 0L,
            reviewReminders = if (data.reviewReminders) 1L else 0L,
            motivationalMessages = if (data.motivationalMessages) 1L else 0L,
            dailyReminderTime = data.dailyReminderTime,
            minimumDueCards = data.minimumDueCards.toLong(),
            successesToAdvance = data.successesToAdvance.toLong(),
            forgotPenalty = data.forgotPenalty.toLong()
        )
    }

    override fun getLanguage(): Flow<Language> {
        return getSettingsFlow()
            .map { settings -> Language.fromCode(settings?.languageCode ?: "en") }
            .onStart {
                settingsRemoteDataSource.getSettingsAsFlow()
                    .catch { /* ignore remote errors */ }
                    .collect { remote -> upsertLocal(remote) }
            }
    }

    private suspend fun updateRemoteOrLocal(updated: SettingsEntityData) {
        var appliedRemote = false
        settingsRemoteDataSource.updateSettingsAsFlow(toRemote(updated))
            .catch { /* ignore remote errors */ }
            .collect { remote ->
                upsertLocal(remote)
                appliedRemote = true
            }
        if (!appliedRemote) {
            insertSettingsData(updated)
        }
    }

    override suspend fun setLanguage(language: Language) {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        val updated = current.copy(languageCode = language.code)
        updateRemoteOrLocal(updated)
    }

    override fun getThemeMode(): Flow<ThemeMode> {
        return getSettingsFlow()
            .map { settings -> ThemeMode.fromString(settings?.themeMode ?: ThemeMode.AUTO.name) }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        val updated = current.copy(themeMode = mode.name)
        updateRemoteOrLocal(updated)
    }

    override fun getNotificationsEnabled(): Flow<Boolean> {
        return getSettingsFlow().map { it?.notificationsEnabled != 0L }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        val updated = current.copy(notificationsEnabled = enabled)
        updateRemoteOrLocal(updated)
    }

    override fun getReviewRemindersEnabled(): Flow<Boolean> {
        return getSettingsFlow().map { it?.reviewReminders != 0L }
    }

    override suspend fun setReviewRemindersEnabled(enabled: Boolean) {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        val updated = current.copy(reviewReminders = enabled)
        updateRemoteOrLocal(updated)
    }

    override fun getMotivationalMessagesEnabled(): Flow<Boolean> {
        return getSettingsFlow().map { it?.motivationalMessages != 0L }
    }

    override suspend fun setMotivationalMessagesEnabled(enabled: Boolean) {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        val updated = current.copy(motivationalMessages = enabled)
        updateRemoteOrLocal(updated)
    }

    override suspend fun getDailyReminderTime(): String {
        return queries.getSettings().awaitAsOneOrNull()?.dailyReminderTime ?: "18:00"
    }

    override suspend fun setDailyReminderTime(time: String) {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        val updated = current.copy(dailyReminderTime = time)
        updateRemoteOrLocal(updated)
    }

    override suspend fun getMinimumDueCards(): Int {
        return queries.getSettings().awaitAsOneOrNull()?.minimumDueCards?.toInt() ?: 5
    }

    override suspend fun setMinimumDueCards(count: Int) {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        val updated = current.copy(minimumDueCards = count)
        updateRemoteOrLocal(updated)
    }

    override fun getSuccessesToAdvance(): Flow<Int> {
        return getSettingsFlow().map { it?.successesToAdvance?.toInt() ?: 1 }
    }

    override suspend fun setSuccessesToAdvance(count: Int) {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        val updated = current.copy(successesToAdvance = count)
        updateRemoteOrLocal(updated)
    }

    override fun getForgotPenalty(): Flow<Int> {
        return getSettingsFlow().map { it?.forgotPenalty?.toInt() ?: 2 }
    }

    override suspend fun setForgotPenalty(levels: Int) {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        val updated = current.copy(forgotPenalty = levels)
        updateRemoteOrLocal(updated)
    }

    override suspend fun updateDailyInsight(date: String, insight: String) {
        queries.updateDailyInsight(date, insight)
    }

    override suspend fun getCachedInsight(): String? {
        return queries.getCachedInsight().awaitAsOneOrNull()?.cachedInsight
    }

    override suspend fun getLastInsightDate(): String? {
        return queries.getLastInsightDate().awaitAsOneOrNull()?.lastInsightDate
    }

    override suspend fun getLastInsightDismissedTime(): Long {
        return queries.getSettings().awaitAsOneOrNull()?.lastInsightDismissedTime ?: 0L
    }

    override suspend fun setLastInsightDismissedTime(timestamp: Long) {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        insertSettingsData(current.copy(lastInsightDismissedTime = timestamp))
    }

    override suspend fun clearInsightData() {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        insertSettingsData(
            current.copy(
                lastInsightDate = null,
                cachedInsight = null,
                lastInsightDismissedTime = 0L
            )
        )
    }

    override suspend fun clearSettings() {
        queries.clearSettings()
    }

    private suspend fun upsertLocal(remote: RemoteSettings) {
        val current = queries.getSettings().awaitAsOneOrNull()?.toData() ?: SettingsEntityData()
        val updated = current.copy(
            languageCode = remote.languageCode,
            themeMode = remote.themeMode,
            notificationsEnabled = remote.notificationsEnabled,
            reviewReminders = remote.reviewReminders,
            motivationalMessages = remote.motivationalMessages,
            dailyReminderTime = remote.dailyReminderTime,
            minimumDueCards = remote.minimumDueCards,
            successesToAdvance = remote.successesToAdvance,
            forgotPenalty = remote.forgotPenalty
        )
        insertSettingsData(updated)
    }

    private fun toRemote(settings: SettingsEntityData): RemoteSettings {
        return RemoteSettings(
            languageCode = settings.languageCode,
            themeMode = settings.themeMode,
            notificationsEnabled = settings.notificationsEnabled,
            reviewReminders = settings.reviewReminders,
            motivationalMessages = settings.motivationalMessages,
            dailyReminderTime = settings.dailyReminderTime,
            minimumDueCards = settings.minimumDueCards,
            successesToAdvance = settings.successesToAdvance,
            forgotPenalty = settings.forgotPenalty
        )
    }
}
