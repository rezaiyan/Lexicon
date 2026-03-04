package domain.notifications.repository

import core.common.Try

interface IPushTokenRepository {
    suspend fun registerToken(token: String): Try<Unit>
    suspend fun deactivateAllTokens(): Try<Unit>
    fun initializeAndRegister()
}



