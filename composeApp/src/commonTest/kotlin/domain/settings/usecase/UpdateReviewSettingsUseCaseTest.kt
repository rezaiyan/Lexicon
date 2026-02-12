package domain.settings.usecase

import domain.settings.model.ReviewSettings
import domain.settings.repository.ISettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateReviewSettingsUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val useCase = UpdateReviewSettingsUseCase(repository)

    @Test
    fun `updating settings stores values in repository`() = runTest {
        val settings = ReviewSettings(successesToAdvance = 3, forgotPenalty = 1)

        useCase(settings)

        assertEquals(3, repository.lastSuccesses)
        assertEquals(1, repository.lastPenalty)
        assertEquals(1, repository.successCalls)
        assertEquals(1, repository.penaltyCalls)
    }

    @Test
    fun `subsequent updates override previous values`() = runTest {
        useCase(ReviewSettings(successesToAdvance = 1, forgotPenalty = 2))
        useCase(ReviewSettings(successesToAdvance = 2, forgotPenalty = 3))

        assertEquals(2, repository.lastSuccesses)
        assertEquals(3, repository.lastPenalty)
        assertEquals(2, repository.successCalls)
        assertEquals(2, repository.penaltyCalls)
    }

    private class FakeSettingsRepository : ISettingsRepository {
        var lastSuccesses: Int? = null
        var lastPenalty: Int? = null
        var successCalls = 0
        var penaltyCalls = 0

        override suspend fun setSuccessesToAdvance(count: Int) {
            lastSuccesses = count
            successCalls++
        }

        override suspend fun setForgotPenalty(levels: Int) {
            lastPenalty = levels
            penaltyCalls++
        }

        override fun getSuccessesToAdvance(): Flow<Int> = flowOf(lastSuccesses ?: ReviewSettings.BALANCED.successesToAdvance)
        override fun getForgotPenalty(): Flow<Int> = flowOf(lastPenalty ?: ReviewSettings.BALANCED.forgotPenalty)
        override fun getLanguage(): Flow<utils.Language> = flowOf(utils.Language.ENGLISH)
        override suspend fun setLanguage(language: utils.Language) {}
        override fun getThemeMode(): Flow<domain.settings.model.ThemeMode> = flowOf(domain.settings.model.ThemeMode.AUTO)
        override suspend fun setThemeMode(mode: domain.settings.model.ThemeMode) {}
        override suspend fun getLastInsightDate(): String? = null
        override suspend fun getCachedInsight(): String? = null
        override suspend fun updateDailyInsight(date: String, insight: String) {}
        override suspend fun getLastInsightDismissedTime(): Long = 0L
        override suspend fun setLastInsightDismissedTime(timestamp: Long) {}
        override suspend fun clearInsightData() {}
        override suspend fun clearSettings() {}
        override fun getNotificationsEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setNotificationsEnabled(enabled: Boolean) {}
        override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setReviewRemindersEnabled(enabled: Boolean) {}
        override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setMotivationalMessagesEnabled(enabled: Boolean) {}
        override suspend fun getDailyReminderTime(): String = "09:00"
        override suspend fun setDailyReminderTime(time: String) {}
        override suspend fun getMinimumDueCards(): Int = 5
        override suspend fun setMinimumDueCards(count: Int) {}
    }
}

