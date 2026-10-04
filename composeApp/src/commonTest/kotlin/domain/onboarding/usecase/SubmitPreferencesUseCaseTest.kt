package domain.onboarding.usecase

import core.common.Try
import core.common.getOrNull
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.ProficiencyLevel
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.repository.IOnboardingRepository
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubmitPreferencesUseCaseTest {

    private val repository = FakeOnboardingRepository()
    private val useCase = SubmitPreferencesUseCase(repository)

    private val preferences = OnboardingPreferences(
        targetLanguage = Language.SPANISH,
        nativeLanguage = Language.ENGLISH,
        level = ProficiencyLevel.BEGINNER,
        interests = listOf("travel", "food"),
    )

    @Test
    fun `invoke returns the suggested words`() = runTest {
        val words = listOf(
            SuggestedVocabulary("Hola", "Hello", "A common greeting", Language.ENGLISH, Language.SPANISH),
            SuggestedVocabulary("Adiós", "Goodbye", "A farewell", Language.ENGLISH, Language.SPANISH),
        )
        repository.submitResult = Try.success(words)

        val result = useCase(preferences)

        assertEquals(words, result.getOrNull())
        assertEquals(preferences, repository.lastPreferences)
    }

    @Test
    fun `invoke when the repository fails returns failure`() = runTest {
        repository.submitResult = Try.failure(RuntimeException("Network error"))

        val result = useCase(preferences)

        assertTrue(result.isFailure)
        assertEquals("Network error", (result as Try.Failure).throwable.message)
    }

    private class FakeOnboardingRepository : IOnboardingRepository {
        var submitResult: Try<List<SuggestedVocabulary>> = Try.success(emptyList())
        var lastPreferences: OnboardingPreferences? = null

        override suspend fun submitPreferences(preferences: OnboardingPreferences): Try<List<SuggestedVocabulary>> {
            lastPreferences = preferences
            return submitResult
        }

        override suspend fun hasCompletedOnboarding(): Try<Boolean> = Try.success(false)
        override suspend fun markOnboardingCompleted(): Try<Unit> = Try.success(Unit)
    }
}
