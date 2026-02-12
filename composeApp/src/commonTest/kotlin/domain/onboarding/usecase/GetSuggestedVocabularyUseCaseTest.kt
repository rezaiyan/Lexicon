package domain.onboarding.usecase

import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.model.SuggestedVocabularyResponse
import domain.onboarding.repository.IOnboardingRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetSuggestedVocabularyUseCaseTest {

    private val repository = FakeOnboardingRepository()
    private val useCase = GetSuggestedVocabularyUseCase(repository)

    @Test
    fun `returns vocabulary from repository`() = runTest {
        val expectedResponse = SuggestedVocabularyResponse(
            suggestedVocabulary = listOf(
                SuggestedVocabulary(
                    originalWord = "Cat",
                    translation = "Gato",
                    description = "A domestic animal",
                    sourceLanguage = "en",
                    targetLanguage = "es"
                ),
                SuggestedVocabulary(
                    originalWord = "Dog",
                    translation = "Perro",
                    description = "A domestic animal",
                    sourceLanguage = "en",
                    targetLanguage = "es"
                )
            ),
            collectionName = "Animals",
            totalCount = 2
        )
        repository.submitResult = Result.success(expectedResponse)

        val preferences = OnboardingPreferences(
            targetLanguage = "es",
            nativeLanguage = "en",
            level = "intermediate",
            interests = listOf("animals")
        )

        val result = useCase(preferences)

        assertTrue(result.isSuccess)
        assertEquals(expectedResponse, result.getOrNull())
        assertEquals(2, result.getOrNull()?.suggestedVocabulary?.size)
        assertEquals("Animals", result.getOrNull()?.collectionName)
    }

    private class FakeOnboardingRepository : IOnboardingRepository {
        var submitResult: Result<SuggestedVocabularyResponse> = Result.success(
            SuggestedVocabularyResponse(emptyList(), "", 0)
        )
        private var onboardingCompleted = false

        override suspend fun submitPreferences(preferences: OnboardingPreferences): Result<SuggestedVocabularyResponse> =
            submitResult

        override suspend fun hasCompletedOnboarding(): Boolean = onboardingCompleted

        override suspend fun markOnboardingCompleted() {
            onboardingCompleted = true
        }
    }
}
