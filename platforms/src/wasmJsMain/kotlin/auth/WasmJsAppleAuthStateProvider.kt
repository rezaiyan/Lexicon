package auth

/**
 * WasmJs stub - Apple Sign In is not supported on web
 */
class WasmJsAppleAuthStateProvider : IAppleAuthStateProvider {
    override suspend fun isSignedInWithApple(): Boolean = false
    override suspend fun getAppleUserIdentifier(): String? = null
    override suspend fun signOutFromApple() {}
}
