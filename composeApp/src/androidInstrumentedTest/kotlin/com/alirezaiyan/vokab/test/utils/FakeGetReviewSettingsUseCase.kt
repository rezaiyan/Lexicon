package com.alirezaiyan.vokab.test.utils

import domain.settings.model.ReviewSettings
import domain.settings.usecase.GetReviewSettingsUseCase
import domain.settings.repository.ISettingsRepository
import kotlinx.coroutines.flow.flowOf

/**
 * Helper to create GetReviewSettingsUseCase for testing
 */
fun createTestReviewSettingsUseCase(
    settings: ReviewSettings = TestUtils.DEFAULT_TEST_SETTINGS
): GetReviewSettingsUseCase {
    val fakeRepository = object : ISettingsRepository {
        override fun getSuccessesToAdvance() = flowOf(settings.successesToAdvance)
        override suspend fun setSuccessesToAdvance(count: Int) {}
        override fun getForgotPenalty() = flowOf(settings.forgotPenalty)
        override suspend fun setForgotPenalty(levels: Int) {}
        
        // Unused methods
        override fun getLanguage() = throw NotImplementedError()
        override suspend fun setLanguage(language: utils.Language) = throw NotImplementedError()
        override fun getThemeMode() = throw NotImplementedError()
        override suspend fun setThemeMode(mode: domain.settings.model.ThemeMode) = throw NotImplementedError()
        override suspend fun getLastInsightDate() = throw NotImplementedError()
        override suspend fun getCachedInsight() = throw NotImplementedError()
        override suspend fun updateDailyInsight(date: String, insight: String) = throw NotImplementedError()
        override suspend fun getLastInsightDismissedTime() = throw NotImplementedError()
        override suspend fun setLastInsightDismissedTime(timestamp: Long) = throw NotImplementedError()
        override fun getNotificationsEnabled() = throw NotImplementedError()
        override suspend fun setNotificationsEnabled(enabled: Boolean) = throw NotImplementedError()
        override fun getReviewRemindersEnabled() = throw NotImplementedError()
        override suspend fun setReviewRemindersEnabled(enabled: Boolean) = throw NotImplementedError()
        override fun getMotivationalMessagesEnabled() = throw NotImplementedError()
        override suspend fun setMotivationalMessagesEnabled(enabled: Boolean) = throw NotImplementedError()
        override suspend fun getDailyReminderTime() = throw NotImplementedError()
        override suspend fun setDailyReminderTime(time: String) = throw NotImplementedError()
        override suspend fun getMinimumDueCards() = throw NotImplementedError()
        override suspend fun setMinimumDueCards(count: Int) = throw NotImplementedError()
        override suspend fun clearInsightData() {}
    }
    
    return GetReviewSettingsUseCase(fakeRepository)
}

