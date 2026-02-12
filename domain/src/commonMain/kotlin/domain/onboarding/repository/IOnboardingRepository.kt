package domain.onboarding.repository

import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabularyResponse

interface IOnboardingRepository {
    suspend fun submitPreferences(preferences: OnboardingPreferences): Result<SuggestedVocabularyResponse>
    suspend fun hasCompletedOnboarding(): Boolean
    suspend fun markOnboardingCompleted()
}
