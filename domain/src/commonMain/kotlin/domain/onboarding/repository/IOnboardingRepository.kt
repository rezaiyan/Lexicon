package domain.onboarding.repository

import core.common.Try
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabulary

interface IOnboardingRepository {
    suspend fun submitPreferences(preferences: OnboardingPreferences): Try<List<SuggestedVocabulary>>
    suspend fun hasCompletedOnboarding(): Try<Boolean>
    suspend fun markOnboardingCompleted(): Try<Unit>

    /** Clears the completion flag, e.g. after the account is deleted, so onboarding runs again. */
    suspend fun resetOnboarding(): Try<Unit>
}
