package auth

/**
 * Android stub - Apple Sign In is iOS-only
 */
class AndroidAppleAuthStateProvider : IAppleAuthStateProvider {
    override suspend fun isSignedInWithApple(): Boolean = false
    override suspend fun getAppleUserIdentifier(): String? = null
    override suspend fun signOutFromApple() {}
}




