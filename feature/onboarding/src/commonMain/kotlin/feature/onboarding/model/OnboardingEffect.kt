package feature.onboarding.model

import domain.onboarding.model.SuggestedVocabulary

sealed interface OnboardingEffect {
    data class NavigateToPreview(val words: List<SuggestedVocabulary>) : OnboardingEffect
    data object NavigateToMain : OnboardingEffect
}
