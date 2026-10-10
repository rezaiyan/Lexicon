package domain.notifications.repository

import core.common.Try

interface IPushTokenRepository {
    suspend fun registerToken(token: String): Try<Unit>
    suspend fun deactivateAllTokens(): Try<Unit>
    suspend fun deactivateCurrentToken(): Try<Unit>
    fun initializeAndRegister()

    /** Whether this device's push token is registered with the server, so server pushes reach it. */
    fun isRegisteredWithServer(): Boolean = false
}



