package feature.onboarding.model

import domain.onboarding.model.SuggestedVocabularyResponse

sealed interface OnboardingEffect {
    data class NavigateToPreview(val response: SuggestedVocabularyResponse) : OnboardingEffect
    data object NavigateToMain : OnboardingEffect
}
