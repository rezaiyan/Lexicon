package domain.onboarding.usecase

import core.common.Try
import core.common.UseCase
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.repository.IOnboardingRepository

class SubmitPreferencesUseCase(
    private val onboardingRepository: IOnboardingRepository
) : UseCase<OnboardingPreferences, List<SuggestedVocabulary>> {
    override suspend operator fun invoke(preferences: OnboardingPreferences): Try<List<SuggestedVocabulary>> {
        return onboardingRepository.submitPreferences(preferences)
    }
}
