package domain.notifications.repository

import domain.common.Try

interface IPushTokenRepository {
    suspend fun registerToken(token: String): Try<Unit>
    suspend fun deactivateAllTokens(): Try<Unit>
    fun initializeAndRegister()
}



