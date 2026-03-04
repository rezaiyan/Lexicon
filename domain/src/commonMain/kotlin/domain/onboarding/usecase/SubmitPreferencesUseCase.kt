package domain.onboarding.usecase

import core.common.Try
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabularyResponse
import domain.onboarding.repository.IOnboardingRepository

class SubmitPreferencesUseCase(
    private val onboardingRepository: IOnboardingRepository
) {
    suspend operator fun invoke(preferences: OnboardingPreferences): Try<SuggestedVocabularyResponse> {
        return onboardingRepository.submitPreferences(preferences)
    }
}
