package feature.settings.model

sealed class DialogState {
    data object None : DialogState()
    data object LanguageSelection : DialogState()
    data object ThemeSelection : DialogState()
    data object NotificationPermission : DialogState()
    data object NotificationSettings : DialogState()
}
