package data.storage

/**
 * Auth token storage — JWT access/refresh tokens and their expiry.
 */
interface AuthTokenStorage {
    suspend fun saveAccessToken(token: String)
    suspend fun saveRefreshToken(token: String)
    fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun clearTokens()
    suspend fun saveTokenExpiresAt(expiresAtMs: Long)
    fun getTokenExpiresAt(): Long
}

/**
 * Onboarding completion tracking.
 */
interface OnboardingStorage {
    suspend fun hasCompletedOnboarding(): Boolean
    suspend fun markOnboardingCompleted()
}

/**
 * Push token tracking — the exact token last registered with the backend,
 * so logout can deactivate that token rather than re-querying the platform SDK.
 */
interface PushTokenStorage {
    suspend fun savePushToken(token: String)
    fun getPushToken(): String?
    suspend fun clearPushToken()
}

/**
 * Local device storage for sensitive/session data.
 * Platform-specific implementations use:
 * - Android: EncryptedSharedPreferences
 * - iOS: Keychain
 * - Desktop: Encrypted file
 */
interface SecureStorage : AuthTokenStorage, OnboardingStorage, PushTokenStorage
