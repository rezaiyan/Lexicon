package presentation.model

import domain.onboarding.model.SuggestedVocabulary

sealed class DialogState {
    data object None : DialogState()
    data object LanguageSelection : DialogState()
    data object ThemeSelection : DialogState()
    data object NotificationPermission : DialogState()
    data object NotificationSettings : DialogState()
}


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

