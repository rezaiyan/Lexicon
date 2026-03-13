package domain.settings.usecase

import core.common.getOrThrow
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetCurrentLanguageUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val useCase = GetCurrentLanguageUseCase(repository)

    @Test
    fun `returns current language`() = runTest {
        repository.language = Language.GERMAN

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(Language.GERMAN, result.getOrThrow())
    }

    @Test
    fun `returns English by default`() = runTest {
        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(Language.ENGLISH, result.getOrThrow())
    }

    @Test
    fun `invoke with Unit params delegates correctly`() = runTest {
        repository.language = Language.SPANISH

        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertEquals(Language.SPANISH, result.getOrThrow())
    }
}

internal class FakeSettingsRepository : ISettingsRepository {
    var language: Language = Language.ENGLISH
    var themeMode: ThemeMode = ThemeMode.AUTO
    var notificationsEnabled = true
    var reviewRemindersEnabled = true
    var motivationalMessagesEnabled = true
    var dailyReminderTime = "09:00"
    var minimumDueCards = 5
    var clearSettingsCalled = false

    override fun getLanguage(): Flow<Language> = flowOf(language)
    override suspend fun setLanguage(language: Language) { this.language = language }
    override fun getThemeMode(): Flow<ThemeMode> = flowOf(themeMode)
    override suspend fun setThemeMode(mode: ThemeMode) { themeMode = mode }
    override suspend fun clearSettings() { clearSettingsCalled = true }
    override fun getNotificationsEnabled(): Flow<Boolean> = flowOf(notificationsEnabled)
    override suspend fun setNotificationsEnabled(enabled: Boolean) { notificationsEnabled = enabled }
    override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(reviewRemindersEnabled)
    override suspend fun setReviewRemindersEnabled(enabled: Boolean) { reviewRemindersEnabled = enabled }
    override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(motivationalMessagesEnabled)
    override suspend fun setMotivationalMessagesEnabled(enabled: Boolean) { motivationalMessagesEnabled = enabled }
    override suspend fun getDailyReminderTime(): String = dailyReminderTime
    override suspend fun setDailyReminderTime(time: String) { dailyReminderTime = time }
    override suspend fun getMinimumDueCards(): Int = minimumDueCards
    override suspend fun setMinimumDueCards(count: Int) { minimumDueCards = count }
}
