package fakes

import core.common.Try
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import utils.Language

class FakeSettingsRepository : ISettingsRepository {
    var language: Language = Language.ENGLISH
    var themeMode: ThemeMode = ThemeMode.AUTO
    var notificationsEnabled = true
    var reviewRemindersEnabled = true
    var motivationalMessagesEnabled = true
    var dailyReminderTime = "09:00"
    var minimumDueCards = 5
    var clearSettingsCalled = false

    override fun getLanguage(): Flow<Language> = flowOf(language)
    override suspend fun setLanguage(language: Language): Try<Unit> {
        this.language = language
        return Try.success(Unit)
    }
    override fun getThemeMode(): Flow<ThemeMode> = flowOf(themeMode)
    override suspend fun setThemeMode(mode: ThemeMode): Try<Unit> { themeMode = mode; return Try.success(Unit) }
    override suspend fun clearSettings(): Try<Unit> { clearSettingsCalled = true; return Try.success(Unit) }
    override fun getNotificationsEnabled(): Flow<Boolean> = flowOf(notificationsEnabled)
    override suspend fun setNotificationsEnabled(enabled: Boolean): Try<Unit> {
        notificationsEnabled = enabled
        return Try.success(Unit)
    }
    override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(reviewRemindersEnabled)
    override suspend fun setReviewRemindersEnabled(enabled: Boolean): Try<Unit> {
        reviewRemindersEnabled = enabled
        return Try.success(Unit)
    }
    override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(motivationalMessagesEnabled)
    override suspend fun setMotivationalMessagesEnabled(enabled: Boolean): Try<Unit> {
        motivationalMessagesEnabled = enabled
        return Try.success(Unit)
    }
    override suspend fun getDailyReminderTime(): Try<String> = Try.success(dailyReminderTime)
    override suspend fun setDailyReminderTime(time: String): Try<Unit> {
        dailyReminderTime = time
        return Try.success(Unit)
    }
    override suspend fun getMinimumDueCards(): Try<Int> = Try.success(minimumDueCards)
    override suspend fun setMinimumDueCards(count: Int): Try<Unit> { minimumDueCards = count; return Try.success(Unit) }
}
