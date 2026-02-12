package auth

/**
 * Platform-specific interface to check Apple Sign-In credential state
 * Allows us to verify if user's Apple ID credential is still valid
 * iOS only - other platforms return false/null
 */
interface IAppleAuthStateProvider {
    /**
     * Check if user has a valid Apple Sign-In credential
     * @return true if user has active Apple Sign-In session
     */
    suspend fun isSignedInWithApple(): Boolean
    
    /**
     * Get the current Apple user identifier if signed in
     * @return User identifier or null if not signed in
     */
    suspend fun getAppleUserIdentifier(): String?
    
    /**
     * Sign out from Apple (invalidate local credential state)
     * Note: Apple doesn't provide a sign-out API, this just clears local state
     */
    suspend fun signOutFromApple()
}