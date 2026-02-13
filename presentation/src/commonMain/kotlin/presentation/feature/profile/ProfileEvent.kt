package presentation.feature.profile

sealed interface ProfileEvent {
    data class LoginWithGoogle(val idToken: String) : ProfileEvent
    data class LoginWithApple(val idToken: String, val fullName: String?, val appleUserId: String) : ProfileEvent
    data object Logout : ProfileEvent
    data object DeleteAccount : ProfileEvent
    data object ClearError : ProfileEvent
}
