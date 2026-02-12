package auth

/**
 * WasmJs stub - Google Sign In is not supported on web
 */
class WasmJsGoogleAuthStateProvider : IGoogleAuthStateProvider {
    override fun isSignedInWithGoogle(): Boolean = false
    override suspend fun getSilentGoogleIdToken(): String? = null
    override suspend fun signOutFromGoogle() {}
}
