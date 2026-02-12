package data.auth.token

import data.storage.SecureStorage

interface ITokenManager {
    suspend fun saveTokens(accessToken: String, refreshToken: String)
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun clearTokens()
    suspend fun hasTokens(): Boolean
}

class TokenManager(
    private val secureStorage: SecureStorage
) : ITokenManager {

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        secureStorage.saveAccessToken(accessToken)
        secureStorage.saveRefreshToken(refreshToken)
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
}

