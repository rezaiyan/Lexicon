package data.settings.repository

import data.core.database.SettingsEntityData
import data.settings.local.ISettingsLocalDataSource
import domain.settings.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsRepositoryImplTest {

    private val localDataSource = FakeSettingsLocalDataSource()

    private fun createRepo() = SettingsRepositoryImpl(localDataSource)

    // --- Language ---

    @Test
    fun `getLanguage returns English when no settings exist`() = runTest {
        val repo = createRepo()

        val language = repo.getLanguage().first()

        assertEquals(Language.ENGLISH, language)
    }

    @Test
    fun `getLanguage returns stored language`() = runTest {
        localDataSource.settings = SettingsEntityData(languageCode = "de")
        val repo = createRepo()

        val language = repo.getLanguage().first()

        assertEquals(Language.GERMAN, language)
    }

    @Test
    fun `setLanguage saves language code`() = runTest {
        val repo = createRepo()

        repo.setLanguage(Language.SPANISH)

        assertEquals("es", localDataSource.settings?.languageCode)
    }

    @Test
    fun `setLanguage preserves other settings`() = runTest {
        localDataSource.settings = SettingsEntityData(
            languageCode = "en",
            themeMode = "DARK",
            notificationsEnabled = false
        )
        val repo = createRepo()

        repo.setLanguage(Language.FRENCH)

        val saved = localDataSource.settings
        assertEquals("fr", saved?.languageCode)
        assertEquals("DARK", saved?.themeMode)
        assertFalse(saved!!.notificationsEnabled)
    }

    // --- Theme Mode ---

    @Test
    fun `getThemeMode returns AUTO when no settings exist`() = runTest {
        val repo = createRepo()

        val mode = repo.getThemeMode().first()

        assertEquals(ThemeMode.AUTO, mode)
    }

    @Test
    fun `getThemeMode returns stored theme mode`() = runTest {
        localDataSource.settings = SettingsEntityData(themeMode = "DARK")
        val repo = createRepo()

        val mode = repo.getThemeMode().first()

        assertEquals(ThemeMode.DARK, mode)
    }

    @Test
    fun `getThemeMode falls back to AUTO for unknown value`() = runTest {
        localDataSource.settings = SettingsEntityData(themeMode = "UNKNOWN")
        val repo = createRepo()

        val mode = repo.getThemeMode().first()

        assertEquals(ThemeMode.AUTO, mode)
    }

    @Test
    fun `setThemeMode saves theme mode name`() = runTest {
        val repo = createRepo()

        repo.setThemeMode(ThemeMode.LIGHT)

        assertEquals("LIGHT", localDataSource.settings?.themeMode)
    }

    // --- Notifications ---

    @Test
    fun `getNotificationsEnabled returns true when no settings exist`() = runTest {
        val repo = createRepo()

        val enabled = repo.getNotificationsEnabled().first()

        assertTrue(enabled)
    }

    @Test
    fun `getNotificationsEnabled returns stored value`() = runTest {
        localDataSource.settings = SettingsEntityData(notificationsEnabled = false)
        val repo = createRepo()

        val enabled = repo.getNotificationsEnabled().first()

        assertFalse(enabled)
    }

    @Test
    fun `setNotificationsEnabled saves value`() = runTest {
        val repo = createRepo()

        repo.setNotificationsEnabled(false)

        assertFalse(localDataSource.settings!!.notificationsEnabled)
    }

    // --- Review Reminders ---

    @Test
    fun `getReviewRemindersEnabled returns true when no settings exist`() = runTest {
        val repo = createRepo()

        val enabled = repo.getReviewRemindersEnabled().first()

        assertTrue(enabled)
    }

    @Test
    fun `setReviewRemindersEnabled saves value`() = runTest {
        val repo = createRepo()

        repo.setReviewRemindersEnabled(false)

        assertFalse(localDataSource.settings!!.reviewReminders)
    }

    // --- Motivational Messages ---

    @Test
    fun `getMotivationalMessagesEnabled returns true when no settings exist`() = runTest {
        val repo = createRepo()

        val enabled = repo.getMotivationalMessagesEnabled().first()

        assertTrue(enabled)
    }

    @Test
    fun `setMotivationalMessagesEnabled saves value`() = runTest {
        val repo = createRepo()

        repo.setMotivationalMessagesEnabled(false)

        assertFalse(localDataSource.settings!!.motivationalMessages)
    }

    // --- Daily Reminder Time ---

    @Test
    fun `getDailyReminderTime returns default when no settings exist`() = runTest {
        val repo = createRepo()

        val time = repo.getDailyReminderTime()

        assertEquals("18:00", time)
    }

    @Test
    fun `getDailyReminderTime returns stored value`() = runTest {
        localDataSource.settings = SettingsEntityData(dailyReminderTime = "09:30")
        val repo = createRepo()

        val time = repo.getDailyReminderTime()

        assertEquals("09:30", time)
    }

    @Test
    fun `setDailyReminderTime saves value`() = runTest {
        val repo = createRepo()

        repo.setDailyReminderTime("07:00")

        assertEquals("07:00", localDataSource.settings?.dailyReminderTime)
    }

    // --- Minimum Due Cards ---

    @Test
    fun `getMinimumDueCards returns default when no settings exist`() = runTest {
        val repo = createRepo()

        val count = repo.getMinimumDueCards()

        assertEquals(5, count)
    }

    @Test
    fun `getMinimumDueCards returns stored value`() = runTest {
        localDataSource.settings = SettingsEntityData(minimumDueCards = 10)
        val repo = createRepo()

        val count = repo.getMinimumDueCards()

        assertEquals(10, count)
    }

    @Test
    fun `setMinimumDueCards saves value`() = runTest {
        val repo = createRepo()

        repo.setMinimumDueCards(15)

        assertEquals(15, localDataSource.settings?.minimumDueCards)
    }

    // --- Clear Settings ---

    @Test
    fun `clearSettings removes all settings`() = runTest {
        localDataSource.settings = SettingsEntityData(
            languageCode = "de",
            themeMode = "DARK",
            notificationsEnabled = false
        )
        val repo = createRepo()

        repo.clearSettings()

        assertNull(localDataSource.settings)
        assertTrue(localDataSource.clearCalled)
    }

    // --- Edge Cases ---

    @Test
    fun `setLanguage creates default settings when none exist`() = runTest {
        val repo = createRepo()

        repo.setLanguage(Language.PERSIAN)

        val saved = localDataSource.settings
        assertEquals("fa", saved?.languageCode)
        assertEquals("AUTO", saved?.themeMode)
        assertTrue(saved!!.notificationsEnabled)
    }

    @Test
    fun `multiple setters accumulate on same settings record`() = runTest {
        val repo = createRepo()

        repo.setLanguage(Language.GERMAN)
        repo.setThemeMode(ThemeMode.DARK)
        repo.setNotificationsEnabled(false)
        repo.setMinimumDueCards(20)

        val saved = localDataSource.settings
        assertEquals("de", saved?.languageCode)
        assertEquals("DARK", saved?.themeMode)
        assertFalse(saved!!.notificationsEnabled)
        assertEquals(20, saved.minimumDueCards)
    }

    // --- Fakes ---

    private class FakeSettingsLocalDataSource : ISettingsLocalDataSource {
        var settings: SettingsEntityData? = null
        var clearCalled = false

        private val settingsFlow = MutableStateFlow<SettingsEntityData?>(null)

        override fun observeSettings(): Flow<SettingsEntityData?> {
            settingsFlow.value = settings
            return settingsFlow
        }

        override suspend fun getSettings(): SettingsEntityData? = settings

        override suspend fun saveSettings(data: SettingsEntityData) {
            settings = data
            settingsFlow.value = data
        }

        override suspend fun clearSettings() {
            settings = null
            settingsFlow.value = null
            clearCalled = true
        }
    }
}
