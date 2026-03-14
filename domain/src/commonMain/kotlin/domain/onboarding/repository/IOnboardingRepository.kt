package domain.onboarding.repository

import core.common.Try
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabularyResponse

interface IOnboardingRepository {
    suspend fun submitPreferences(preferences: OnboardingPreferences): Try<SuggestedVocabularyResponse>
    suspend fun hasCompletedOnboarding(): Try<Boolean>
    suspend fun markOnboardingCompleted(): Try<Unit>
}
