package data.storage

/**
 * Secure storage interface for sensitive data like JWT tokens
 * Platform-specific implementations use:
 * - Android: EncryptedSharedPreferences
 * - iOS: Keychain
 * - Desktop: Encrypted file
 */
interface SecureStorage {
    suspend fun saveAccessToken(token: String)
    suspend fun saveRefreshToken(token: String)
    fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun clearTokens()

    // Token expiry tracking for proactive refresh
    suspend fun saveTokenExpiresAt(expiresAtMs: Long)
    fun getTokenExpiresAt(): Long

    // Onboarding completion tracking
    suspend fun hasCompletedOnboarding(): Boolean
    suspend fun markOnboardingCompleted()
}
