package expects

/**
 * WasmJs implementation of AppleSignInHelper
 * No-op on web platform - Apple Sign In is not supported
 */
actual class AppleSignInHelper {
    actual fun signIn(
        onSuccess: (idToken: String, fullName: String?, appleUserId: String) -> Unit,
        onFailure: (error: String) -> Unit
    ) {
        onFailure("Apple Sign In is not available on web")
    }
}
