package domain.notifications.usecase

import core.common.Try
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.word.model.ProgressStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleNotificationsUseCaseTest {

    private val notificationRepo = FakeNotificationRepository()
    private val settingsRepo = FakeSettingsRepo()

    private fun createUseCase() = ScheduleNotificationsUseCase(notificationRepo, settingsRepo)

    @Test
    fun `schedules reminder when enabled and due cards meet minimum`() = runTest {
        settingsRepo.reviewRemindersOn = true
        settingsRepo.minDueCards = 5
        val stats = ProgressStats(dueCards = 10, totalWords = 20)
        val useCase = createUseCase()

        val result = useCase(stats, { "Title $it" }, { "Message $it" })

        assertTrue(result.isSuccess)
        assertTrue(notificationRepo.scheduledReminder)
        assertEquals(10, notificationRepo.lastScheduledDueCount)
        assertEquals("Title 10", notificationRepo.lastScheduledTitle)
        assertEquals("Message 10", notificationRepo.lastScheduledMessage)
        assertEquals(24 * 60, notificationRepo.lastScheduledDelayMinutes)
    }

    @Test
    fun `does not schedule when reminders disabled`() = runTest {
        settingsRepo.reviewRemindersOn = false
        settingsRepo.minDueCards = 5
        val stats = ProgressStats(dueCards = 10, totalWords = 20)
        val useCase = createUseCase()

        val result = useCase(stats, { "Title" }, { "Message" })

        assertTrue(result.isSuccess)
        assertFalse(notificationRepo.scheduledReminder)
    }

    @Test
    fun `does not schedule when due cards below minimum`() = runTest {
        settingsRepo.reviewRemindersOn = true
        settingsRepo.minDueCards = 10
        val stats = ProgressStats(dueCards = 5, totalWords = 20)
        val useCase = createUseCase()

        val result = useCase(stats, { "Title" }, { "Message" })

        assertTrue(result.isSuccess)
        assertFalse(notificationRepo.scheduledReminder)
    }

    @Test
    fun `does not schedule twice due to stateful hasScheduled flag`() = runTest {
        settingsRepo.reviewRemindersOn = true
        settingsRepo.minDueCards = 1
        val stats = ProgressStats(dueCards = 5, totalWords = 20)
        val useCase = createUseCase()

        useCase(stats, { "Title" }, { "Message" })
        notificationRepo.scheduledReminder = false // reset
        useCase(stats, { "Title" }, { "Message" })

        assertFalse(notificationRepo.scheduledReminder) // Should not schedule again
    }

    @Test
    fun `invoke with Params delegates correctly`() = runTest {
        settingsRepo.reviewRemindersOn = true
        settingsRepo.minDueCards = 1
        val stats = ProgressStats(dueCards = 3, totalWords = 10)
        val useCase = createUseCase()

        val result = useCase(ScheduleNotificationsUseCase.Params(stats, { "T" }, { "M" }))

        assertTrue(result.isSuccess)
    }

    private class FakeSettingsRepo : ISettingsRepository {
        var reviewRemindersOn = true
        var minDueCards = 5
        override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(reviewRemindersOn)
        override suspend fun getMinimumDueCards(): Try<Int> = Try.success(minDueCards)

        override fun getLanguage(): Flow<Language> = flowOf(Language.ENGLISH)
        override suspend fun setLanguage(language: Language): Try<Unit> = Try.success(Unit)
        override fun getThemeMode(): Flow<ThemeMode> = flowOf(ThemeMode.AUTO)
        override suspend fun setThemeMode(mode: ThemeMode): Try<Unit> = Try.success(Unit)
        override suspend fun clearSettings(): Try<Unit> = Try.success(Unit)
        override fun getNotificationsEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setNotificationsEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override suspend fun setReviewRemindersEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setMotivationalMessagesEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override suspend fun getDailyReminderTime(): Try<String> = Try.success("09:00")
        override suspend fun setDailyReminderTime(time: String): Try<Unit> = Try.success(Unit)
        override suspend fun setMinimumDueCards(count: Int): Try<Unit> = Try.success(Unit)
    }
}
