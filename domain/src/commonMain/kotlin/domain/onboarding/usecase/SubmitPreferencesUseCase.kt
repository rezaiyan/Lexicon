package domain.onboarding.usecase

import core.common.Try
import core.common.UseCase
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabularyResponse
import domain.onboarding.repository.IOnboardingRepository

class SubmitPreferencesUseCase(
    private val onboardingRepository: IOnboardingRepository
) : UseCase<OnboardingPreferences, SuggestedVocabularyResponse> {
    override suspend operator fun invoke(preferences: OnboardingPreferences): Try<SuggestedVocabularyResponse> {
        return onboardingRepository.submitPreferences(preferences)
    }
}
