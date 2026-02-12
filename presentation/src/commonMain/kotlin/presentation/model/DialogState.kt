package presentation.model

sealed class DialogState {
    data object None : DialogState()
    data object LanguageSelection : DialogState()
    data object ThemeSelection : DialogState()
    data object NotificationPermission : DialogState()
    data object NotificationSettings : DialogState()
    data object ReviewSettings : DialogState()
}


sealed class AppUiState {
    data object Splash : AppUiState()
    data object AuthGate : AppUiState()
    data object Onboarding : AppUiState()
    data object Ready : AppUiState()
}

sealed class UiMessage {
    data object ReviewComplete : UiMessage()
    data object WordDeleted : UiMessage()
}

