package domain.notifications.repository

interface IPushTokenRepository {
    suspend fun registerToken(token: String): Result<Unit>
    suspend fun deactivateAllTokens(): Result<Unit>
    fun initializeAndRegister()
}



