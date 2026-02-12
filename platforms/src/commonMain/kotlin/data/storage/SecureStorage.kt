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
    
    // Daily insight push notification data
    suspend fun storeDailyInsightData(insightId: String, date: String, timestamp: Long)
    suspend fun getDailyInsightData(): DailyInsightData?
    suspend fun clearDailyInsightData()

    // Onboarding completion tracking
    suspend fun hasCompletedOnboarding(): Boolean
    suspend fun markOnboardingCompleted()
}

data class DailyInsightData(
    val insightId: String,
    val date: String,
    val timestamp: Long
)


