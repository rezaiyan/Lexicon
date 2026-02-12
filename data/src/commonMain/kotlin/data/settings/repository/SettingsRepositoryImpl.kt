package data.settings.repository

import data.core.database.SettingsEntity
import data.core.database.LexiconDao
import data.settings.remote.SettingsRemoteDataSource
import data.settings.remote.model.RemoteSettings
import domain.settings.repository.ISettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import domain.settings.model.ThemeMode
import utils.Language

class SettingsRepositoryImpl(
    private val dao: LexiconDao,
    private val settingsRemoteDataSource: SettingsRemoteDataSource
) : ISettingsRepository {

    private fun getSettingsFlow(): Flow<SettingsEntity?> = dao.getSettings()

    override fun getLanguage(): Flow<Language> {
        return getSettingsFlow()
            .map { settings -> Language.fromCode(settings?.languageCode ?: "en") }
            .onStart {
                settingsRemoteDataSource.getSettingsAsFlow()
                    .catch { /* ignore remote errors */ }
                    .collect { remote -> upsertLocal(remote) }
            }
    }

    private suspend fun updateRemoteOrLocal(updated: SettingsEntity) {
        var appliedRemote = false
        settingsRemoteDataSource.updateSettingsAsFlow(toRemote(updated))
            .catch { /* ignore remote errors */ }
            .collect { remote ->
                upsertLocal(remote)
                appliedRemote = true
            }
        if (!appliedRemote) {
            dao.insertSettings(updated)
        }
    }

    override suspend fun setLanguage(language: Language) {
        val current = dao.getSettingsOnce() ?: SettingsEntity()
        val updated = current.copy(languageCode = language.code)
        updateRemoteOrLocal(updated)
    }

    override fun getThemeMode(): Flow<ThemeMode> {
        return getSettingsFlow()
            .map { settings -> ThemeMode.fromString(settings?.themeMode ?: ThemeMode.AUTO.name) }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        val current = dao.getSettingsOnce() ?: SettingsEntity()
        val updated = current.copy(themeMode = mode.name)
        updateRemoteOrLocal(updated)
    }

    override fun getNotificationsEnabled(): Flow<Boolean> {
        return getSettingsFlow().map { it?.notificationsEnabled ?: true }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        val current = dao.getSettingsOnce() ?: SettingsEntity()
        val updated = current.copy(notificationsEnabled = enabled)
        updateRemoteOrLocal(updated)
    }

    override fun getReviewRemindersEnabled(): Flow<Boolean> {
        return getSettingsFlow().map { it?.reviewReminders ?: true }
    }

    override suspend fun setReviewRemindersEnabled(enabled: Boolean) {
        val current = dao.getSettingsOnce() ?: SettingsEntity()
        val updated = current.copy(reviewReminders = enabled)
        updateRemoteOrLocal(updated)
    }

    override fun getMotivationalMessagesEnabled(): Flow<Boolean> {
        return getSettingsFlow().map { it?.motivationalMessages ?: true }
    }

    override suspend fun setMotivationalMessagesEnabled(enabled: Boolean) {
        val current = dao.getSettingsOnce() ?: SettingsEntity()
        val updated = current.copy(motivationalMessages = enabled)
        updateRemoteOrLocal(updated)
    }

    override suspend fun getDailyReminderTime(): String {
        return dao.getSettingsOnce()?.dailyReminderTime ?: "18:00"
    }

    override suspend fun setDailyReminderTime(time: String) {
        val current = dao.getSettingsOnce() ?: SettingsEntity()
        val updated = current.copy(dailyReminderTime = time)
        updateRemoteOrLocal(updated)
    }

    override suspend fun getMinimumDueCards(): Int {
        return dao.getSettingsOnce()?.minimumDueCards ?: 5
    }

    override suspend fun setMinimumDueCards(count: Int) {
        val current = dao.getSettingsOnce() ?: SettingsEntity()
        val updated = current.copy(minimumDueCards = count)
        updateRemoteOrLocal(updated)
    }

    override fun getSuccessesToAdvance(): Flow<Int> {
        return getSettingsFlow().map { it?.successesToAdvance ?: 1 }
    }

    override suspend fun setSuccessesToAdvance(count: Int) {
        val current = dao.getSettingsOnce() ?: SettingsEntity()
        val updated = current.copy(successesToAdvance = count)
        updateRemoteOrLocal(updated)
    }

    override fun getForgotPenalty(): Flow<Int> {
        return getSettingsFlow().map { it?.forgotPenalty ?: 2 }
    }

    override suspend fun setForgotPenalty(levels: Int) {
        val current = dao.getSettingsOnce() ?: SettingsEntity()
        val updated = current.copy(forgotPenalty = levels)
        updateRemoteOrLocal(updated)
    }

    override suspend fun updateDailyInsight(date: String, insight: String) {
        dao.updateDailyInsight(date, insight)
    }

    override suspend fun getCachedInsight(): String? {
        return dao.getCachedInsight()
    }

    override suspend fun getLastInsightDate(): String? {
        return dao.getLastInsightDate()
    }

    override suspend fun getLastInsightDismissedTime(): Long {
        return dao.getSettingsOnce()?.lastInsightDismissedTime ?: 0L
    }

    override suspend fun setLastInsightDismissedTime(timestamp: Long) {
        val current = dao.getSettingsOnce() ?: SettingsEntity()
        dao.insertSettings(current.copy(lastInsightDismissedTime = timestamp))
    }

    override suspend fun clearInsightData() {
        val current = dao.getSettingsOnce() ?: SettingsEntity()
        dao.insertSettings(
            current.copy(
                lastInsightDate = null,
                cachedInsight = null,
                lastInsightDismissedTime = 0L
            )
        )
    }

    override suspend fun clearSettings() {
        dao.clearSettings()
    }

    private suspend fun upsertLocal(remote: RemoteSettings) {
        val current = dao.getSettingsOnce() ?: SettingsEntity()
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
        dao.insertSettings(updated)
    }

    private fun toRemote(settings: SettingsEntity): RemoteSettings {
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
