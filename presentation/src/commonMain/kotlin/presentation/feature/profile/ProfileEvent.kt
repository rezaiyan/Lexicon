package presentation.feature.profile

sealed interface ProfileEvent {
    data object Logout : ProfileEvent
    data object DeleteAccount : ProfileEvent
    data object ClearError : ProfileEvent
}
