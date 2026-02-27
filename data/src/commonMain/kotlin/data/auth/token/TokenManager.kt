package data.auth.token

import data.storage.SecureStorage
import kotlin.time.Clock

interface ITokenManager {
    suspend fun saveTokens(accessToken: String, refreshToken: String, expiresInMs: Long = 0L)
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun clearTokens()
    suspend fun hasTokens(): Boolean

    /**
     * Returns the absolute timestamp (epoch ms) when the access token expires.
     * Returns 0 if unknown.
     */
    fun getTokenExpiresAt(): Long
}

class TokenManager(
    private val secureStorage: SecureStorage
) : ITokenManager {

    override suspend fun saveTokens(accessToken: String, refreshToken: String, expiresInMs: Long) {
        secureStorage.saveAccessToken(accessToken)
        secureStorage.saveRefreshToken(refreshToken)
        if (expiresInMs > 0) {
            secureStorage.saveTokenExpiresAt(
                Clock.System.now().toEpochMilliseconds() + expiresInMs
            )
        }
    }

    override suspend fun getAccessToken(): String? {
        return secureStorage.getAccessToken()
    }

    override suspend fun getRefreshToken(): String? {
        return secureStorage.getRefreshToken()
    }

    override suspend fun clearTokens() {
        secureStorage.clearTokens()
    }

    override suspend fun hasTokens(): Boolean {
        return secureStorage.getAccessToken() != null
    }

    override fun getTokenExpiresAt(): Long {
        return secureStorage.getTokenExpiresAt()
    }
}
