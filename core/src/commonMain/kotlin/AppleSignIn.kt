package expects

/**
 * Platform-specific Apple Sign In implementation
 */
expect class AppleSignInHelper {
    /**
     * Initiates Apple Sign In flow
     * @param onSuccess Called with (idToken, fullName, appleUserId) when sign in succeeds
     * @param onFailure Called with error message when sign in fails
     */
    fun signIn(
        onSuccess: (idToken: String, fullName: String?, appleUserId: String) -> Unit,
        onFailure: (error: String) -> Unit
    )
}