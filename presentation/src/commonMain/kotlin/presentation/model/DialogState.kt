package presentation.model

import domain.onboarding.model.SuggestedVocabulary

sealed class AppUiState {
    data object Splash : AppUiState()
    data object Onboarding : AppUiState()
    data class VocabularyPreview(val words: List<SuggestedVocabulary>) : AppUiState()
    data class AuthGate(val pendingVocabulary: List<SuggestedVocabulary> = emptyList()) : AppUiState()
    data object Ready : AppUiState()
}

sealed class UiMessage {
    data object ReviewComplete : UiMessage()
    data object WordDeleted : UiMessage()
}
