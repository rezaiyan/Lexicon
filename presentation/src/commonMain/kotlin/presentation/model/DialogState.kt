package presentation.model

import domain.onboarding.model.SuggestedVocabulary
import feature.auth.AuthPhase

sealed class AppUiState {
    data class Auth(
        val phase: AuthPhase = AuthPhase.Verifying,
        val pendingVocabulary: List<SuggestedVocabulary> = emptyList(),
        val needsOnboardingCheck: Boolean = false,
    ) : AppUiState()
    data object Onboarding : AppUiState()
    data class VocabularyPreview(val words: List<SuggestedVocabulary>) : AppUiState()
    data object Ready : AppUiState()
}