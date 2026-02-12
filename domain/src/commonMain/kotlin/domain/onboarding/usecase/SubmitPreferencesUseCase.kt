package domain.onboarding.usecase

import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabularyResponse
import domain.onboarding.repository.IOnboardingRepository

class SubmitPreferencesUseCase(
    private val onboardingRepository: IOnboardingRepository
) {
    suspend operator fun invoke(preferences: OnboardingPreferences): Result<SuggestedVocabularyResponse> =
        onboardingRepository.submitPreferences(preferences)
}
