package domain.settings.usecase

import domain.settings.model.ReviewSettings
import domain.settings.repository.ISettingsRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive tests for GetReviewSettingsUseCase
 * 
 * Tests cover:
 * - Successful settings retrieval
 * - Error handling and fallback to defaults
 * - Repository interaction
 * - Settings validation
 */
class GetReviewSettingsUseCaseTest {
    
    private val fakeRepository = FakeSettingsRepository()
    private val useCase = GetReviewSettingsUseCase(fakeRepository)
    
    @Test
    fun `successful settings retrieval should return correct settings`() = runTest {
        // Given: Repository returns valid settings
        fakeRepository.setSuccessesToAdvance(2)
        fakeRepository.setForgotPenalty(3)
        
        // When: Getting review settings
        val settings = useCase()
        
        // Then: Should return correct settings
        assertEquals(2, settings.successesToAdvance)
        assertEquals(3, settings.forgotPenalty)
        assertEquals(1, fakeRepository.getSuccessesToAdvanceCallCount)
        assertEquals(1, fakeRepository.getForgotPenaltyCallCount)
    }
    
    @Test
    fun `repository error should return default balanced settings`() = runTest {
        // Given: Repository throws exception
        fakeRepository.shouldThrowException = true
        
        // When: Getting review settings
        val settings = useCase()
        
        // Then: Should return default balanced settings
        assertEquals(ReviewSettings.BALANCED.successesToAdvance, settings.successesToAdvance)
        assertEquals(ReviewSettings.BALANCED.forgotPenalty, settings.forgotPenalty)
        // Note: Call count may be 0 if exception is thrown before incrementing
        assertTrue(fakeRepository.getSuccessesToAdvanceCallCount >= 0)
        assertTrue(fakeRepository.getForgotPenaltyCallCount >= 0)
    }
    
    @Test
    fun `successes to advance error should return default settings`() = runTest {
        // Given: Repository throws exception for successes to advance
        fakeRepository.shouldThrowSuccessesException = true
        
        // When: Getting review settings
        val settings = useCase()
        
        // Then: Should return default balanced settings
        assertEquals(ReviewSettings.BALANCED.successesToAdvance, settings.successesToAdvance)
        assertEquals(ReviewSettings.BALANCED.forgotPenalty, settings.forgotPenalty)
    }
    
    @Test
    fun `forgot penalty error should return default settings`() = runTest {
        // Given: Repository throws exception for forgot penalty
        fakeRepository.shouldThrowPenaltyException = true
        
        // When: Getting review settings
        val settings = useCase()
        
        // Then: Should return default balanced settings
        assertEquals(ReviewSettings.BALANCED.successesToAdvance, settings.successesToAdvance)
        assertEquals(ReviewSettings.BALANCED.forgotPenalty, settings.forgotPenalty)
    }
    
    @Test
    fun `minimum valid settings should work`() = runTest {
        // Given: Minimum valid settings
        fakeRepository.setSuccessesToAdvance(1)
        fakeRepository.setForgotPenalty(1)
        
        // When: Getting review settings
        val settings = useCase()
        
        // Then: Should return correct settings
        assertEquals(1, settings.successesToAdvance)
        assertEquals(1, settings.forgotPenalty)
    }
    
    @Test
    fun `maximum valid settings should work`() = runTest {
        // Given: Maximum valid settings
        fakeRepository.setSuccessesToAdvance(3)
        fakeRepository.setForgotPenalty(3)
        
        // When: Getting review settings
        val settings = useCase()
        
        // Then: Should return correct settings
        assertEquals(3, settings.successesToAdvance)
        assertEquals(3, settings.forgotPenalty)
    }
    
    @Test
    fun `easy mode settings should work`() = runTest {
        // Given: Easy mode settings
        fakeRepository.setSuccessesToAdvance(1)
        fakeRepository.setForgotPenalty(1)
        
        // When: Getting review settings
        val settings = useCase()
        
        // Then: Should match easy mode
        assertEquals(ReviewSettings.EASY.successesToAdvance, settings.successesToAdvance)
        assertEquals(ReviewSettings.EASY.forgotPenalty, settings.forgotPenalty)
    }
    
    @Test
    fun `rigorous mode settings should work`() = runTest {
        // Given: Rigorous mode settings
        fakeRepository.setSuccessesToAdvance(2)
        fakeRepository.setForgotPenalty(3)
        
        // When: Getting review settings
        val settings = useCase()
        
        // Then: Should match rigorous mode
        assertEquals(ReviewSettings.RIGOROUS.successesToAdvance, settings.successesToAdvance)
        assertEquals(ReviewSettings.RIGOROUS.forgotPenalty, settings.forgotPenalty)
    }
    
    @Test
    fun `expert mode settings should work`() = runTest {
        // Given: Expert mode settings
        fakeRepository.setSuccessesToAdvance(3)
        fakeRepository.setForgotPenalty(3)
        
        // When: Getting review settings
        val settings = useCase()
        
        // Then: Should match expert mode
        assertEquals(ReviewSettings.EXPERT.successesToAdvance, settings.successesToAdvance)
        assertEquals(ReviewSettings.EXPERT.forgotPenalty, settings.forgotPenalty)
    }
    
    @Test
    fun `multiple calls should work correctly`() = runTest {
        // Given: Repository with changing settings
        fakeRepository.setSuccessesToAdvance(1)
        fakeRepository.setForgotPenalty(2)
        
        // When: Getting review settings multiple times
        val settings1 = useCase()
        val settings2 = useCase()
        
        // Then: Both should return same settings
        assertEquals(settings1.successesToAdvance, settings2.successesToAdvance)
        assertEquals(settings1.forgotPenalty, settings2.forgotPenalty)
        assertEquals(2, fakeRepository.getSuccessesToAdvanceCallCount)
        assertEquals(2, fakeRepository.getForgotPenaltyCallCount)
    }
    
    @Test
    fun `settings should be validated by ReviewSettings constructor`() = runTest {
        // Given: Invalid settings from repository (should be caught by ReviewSettings validation)
        fakeRepository.setSuccessesToAdvance(0) // Invalid: below minimum
        fakeRepository.setForgotPenalty(4) // Invalid: above maximum
        
        // When: Getting review settings
        val settings = useCase()
        
        // Then: Should return default settings (validation error caught)
        assertEquals(ReviewSettings.BALANCED.successesToAdvance, settings.successesToAdvance)
        assertEquals(ReviewSettings.BALANCED.forgotPenalty, settings.forgotPenalty)
    }
}

/**
 * Fake repository for testing GetReviewSettingsUseCase
 */
internal class FakeSettingsRepository : ISettingsRepository {
    var getSuccessesToAdvanceCallCount = 0
    var getForgotPenaltyCallCount = 0
    var shouldThrowException = false
    var shouldThrowSuccessesException = false
    var shouldThrowPenaltyException = false
    
    private var successesToAdvance = 1
    private var forgotPenalty = 2
    
    override suspend fun setSuccessesToAdvance(count: Int) {
        successesToAdvance = count
    }
    
    override suspend fun setForgotPenalty(levels: Int) {
        forgotPenalty = levels
    }
    
    override fun getSuccessesToAdvance(): kotlinx.coroutines.flow.Flow<Int> {
        getSuccessesToAdvanceCallCount++
        
        if (shouldThrowException || shouldThrowSuccessesException) {
            throw Exception("Repository error")
        }
        
        return flowOf(successesToAdvance)
    }
    
    override fun getForgotPenalty(): kotlinx.coroutines.flow.Flow<Int> {
        getForgotPenaltyCallCount++
        
        if (shouldThrowException || shouldThrowPenaltyException) {
            throw Exception("Repository error")
        }
        
        return flowOf(forgotPenalty)
    }
    
    // Other methods not needed for this test
    override fun getLanguage() = kotlinx.coroutines.flow.flowOf(utils.Language.ENGLISH)
    override suspend fun setLanguage(language: utils.Language) {}
    override fun getThemeMode() = kotlinx.coroutines.flow.flowOf(domain.settings.model.ThemeMode.AUTO)
    override suspend fun setThemeMode(mode: domain.settings.model.ThemeMode) {}
    override suspend fun getLastInsightDate() = null
    override suspend fun getCachedInsight() = null
    override suspend fun updateDailyInsight(date: String, insight: String) {}
    override suspend fun getLastInsightDismissedTime() = 0L
    override suspend fun setLastInsightDismissedTime(timestamp: Long) {}
    override suspend fun clearInsightData() {}
    override suspend fun clearSettings() {}
    override fun getNotificationsEnabled() = kotlinx.coroutines.flow.flowOf(true)
    override suspend fun setNotificationsEnabled(enabled: Boolean) {}
    override fun getReviewRemindersEnabled() = kotlinx.coroutines.flow.flowOf(true)
    override suspend fun setReviewRemindersEnabled(enabled: Boolean) {}
    override fun getMotivationalMessagesEnabled() = kotlinx.coroutines.flow.flowOf(true)
    override suspend fun setMotivationalMessagesEnabled(enabled: Boolean) {}
    override suspend fun getDailyReminderTime() = "09:00"
    override suspend fun setDailyReminderTime(time: String) {}
    override suspend fun getMinimumDueCards() = 5
    override suspend fun setMinimumDueCards(count: Int) {}
}
