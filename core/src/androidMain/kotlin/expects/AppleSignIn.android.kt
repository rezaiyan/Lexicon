package expects

actual class AppleSignInHelper {
    actual fun signIn(
        onSuccess: (idToken: String, fullName: String?, appleUserId: String) -> Unit,
        onFailure: (error: String) -> Unit
    ) {
        onFailure("Apple Sign In is only available on iOS")
    }
}



