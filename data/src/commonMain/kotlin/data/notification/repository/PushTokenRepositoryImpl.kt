package data.notification.repository

import data.notification.remote.IPushNotificationDataSource
import data.notification.remote.model.Platform
import data.notification.remote.model.RegisterPushTokenRequest
import core.common.Try
import core.common.onFailure
import core.common.onSuccess
import domain.notifications.repository.IPushTokenRepository
import expects.logNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pushnotification.IPushTokenManager

class PushTokenRepositoryImpl(
    private val pushTokenManager: IPushTokenManager,
    private val pushNotificationDataSource: IPushNotificationDataSource,
    private val platform: Platform
) : IPushTokenRepository {

    override fun initializeAndRegister() {
        logNetwork("RegisterPushToken", " Initializing push notification manager")

        pushTokenManager.initialize { token ->
            logNetwork("RegisterPushToken", " Token received, registering with backend...")

            CoroutineScope(Dispatchers.Default).launch {
                registerToken(token)
            }
        }
    }

    override suspend fun registerToken(token: String): Try<Unit> {
        logNetwork("RegisterPushToken", " Sending token to backend...")

        val request = RegisterPushTokenRequest(
            token = token,
            platform = platform,
            deviceId = null
        )

        return pushNotificationDataSource.registerPushToken(request).also { result ->
            result.onSuccess {
                logNetwork("RegisterPushToken", " Push token registered successfully")
            }.onFailure { error ->
                logNetwork("RegisterPushToken", " Failed to register push token: ${error.message}")
            }
        }
    }

    override suspend fun deactivateAllTokens(): Try<Unit> {
        logNetwork("RegisterPushToken", " Deactivating all push tokens...")
        return pushNotificationDataSource.deactivateAllTokens()
    }
}

