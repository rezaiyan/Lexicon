package domain.onboarding.usecase

import domain.common.Try
import domain.common.exceptionOrNull
import domain.common.getOrNull
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.model.SuggestedVocabularyResponse
import domain.onboarding.repository.IOnboardingRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubmitPreferencesUseCaseTest {

    private val repository = FakeOnboardingRepository()
    private val useCase = SubmitPreferencesUseCase(repository)

    @Test
    fun `returns success with suggested vocabulary`() = runTest {
        val expectedResponse = SuggestedVocabularyResponse(
            suggestedVocabulary = listOf(
                SuggestedVocabulary(
                    originalWord = "Hello",
                    translation = "Hola",
                    description = "A common greeting",
                    sourceLanguage = "en",
                    targetLanguage = "es"
                ),
                SuggestedVocabulary(
                    originalWord = "Goodbye",
                    translation = "Adiós",
                    description = "A farewell",
                    sourceLanguage = "en",
                    targetLanguage = "es"
                )
            ),
            targetLanguage = "es",
            nativeLanguage = "en",
            currentLevel = "beginner"
        )
        repository.submitResult = Try.success(expectedResponse)

        val preferences = OnboardingPreferences(
            targetLanguage = "es",
            nativeLanguage = "en",
            level = "beginner",
            interests = listOf("travel", "food")
        )

        val result = useCase(preferences)

        assertTrue(result.isSuccess)
        assertEquals(expectedResponse, result.getOrNull())
    }

    @Test
    fun `returns failure on error`() = runTest {
        val exception = RuntimeException("Network error")
        repository.submitResult = Try.failure(exception)

        val preferences = OnboardingPreferences(
            targetLanguage = "es",
            nativeLanguage = "en",
            level = "beginner"
        )

        val result = useCase(preferences)

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    private class FakeOnboardingRepository : IOnboardingRepository {
        var submitResult: Try<SuggestedVocabularyResponse> = Try.success(
            SuggestedVocabularyResponse(emptyList(), "", "", "")
        )
        private var onboardingCompleted = false

        override suspend fun submitPreferences(preferences: OnboardingPreferences): Try<SuggestedVocabularyResponse> =
            submitResult

        override suspend fun hasCompletedOnboarding(): Boolean = onboardingCompleted

        override suspend fun markOnboardingCompleted() {
            onboardingCompleted = true
        }
    }
}
