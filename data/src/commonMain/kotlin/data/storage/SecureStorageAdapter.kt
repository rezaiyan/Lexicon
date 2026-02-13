package data.storage

import domain.auth.storage.ISecureStorage
import data.storage.SecureStorage as PlatformSecureStorage

/**
 * Adapter that bridges the platform-specific SecureStorage to the domain ISecureStorage interface.
 * This maintains Clean Architecture by allowing domain layer to depend on its own abstractions.
 */
class SecureStorageAdapter(
    private val platformStorage: PlatformSecureStorage
) : ISecureStorage {

    override suspend fun clearTokens() {
        platformStorage.clearTokens()
    }

    override suspend fun clearDailyInsightData() {
        platformStorage.clearDailyInsightData()
    }
}
