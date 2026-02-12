package auth

/**
 * Interface for Google authentication state provider
 * Platform-specific implementations should provide concrete implementations
 */
interface IGoogleAuthStateProvider {
    fun isSignedInWithGoogle(): Boolean
    suspend fun getSilentGoogleIdToken(): String?
    suspend fun signOutFromGoogle()
}

