package presentation.model

import domain.onboarding.model.SuggestedVocabulary

sealed class AppUiState {
    data object Splash : AppUiState()
    data object Onboarding : AppUiState()
    data class VocabularyPreview(val words: List<SuggestedVocabulary>) : AppUiState()
    data class AuthGate(
        val pendingVocabulary: List<SuggestedVocabulary> = emptyList(),
        val needsOnboardingCheck: Boolean = false
    ) : AppUiState()
    data object Ready : AppUiState()
}