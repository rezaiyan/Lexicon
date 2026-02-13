package domain.onboarding.repository

import domain.common.Try
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabularyResponse

interface IOnboardingRepository {
    suspend fun submitPreferences(preferences: OnboardingPreferences): Try<SuggestedVocabularyResponse>
    suspend fun hasCompletedOnboarding(): Boolean
    suspend fun markOnboardingCompleted()
}
