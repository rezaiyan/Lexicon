package domain.auth.storage

/**
 * Domain abstraction for secure storage operations needed by auth use cases.
 * Platform implementations are provided by the platforms/data layers.
 */
interface ISecureStorage {
    /**
     * Clears all stored authentication tokens (access and refresh tokens).
     */
    suspend fun clearTokens()

    /**
     * Clears daily insight push notification data.
     */
    suspend fun clearDailyInsightData()
}
