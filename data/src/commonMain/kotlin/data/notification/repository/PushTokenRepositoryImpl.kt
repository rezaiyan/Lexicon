package data.notification.repository

import data.notification.remote.PushNotificationDataSource
import data.notification.remote.model.Platform
import data.notification.remote.model.RegisterPushTokenRequest
import domain.notifications.repository.IPushTokenRepository
import expects.logNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pushnotification.IPushTokenManager

class PushTokenRepositoryImpl(
    private val pushTokenManager: IPushTokenManager,
    private val pushNotificationDataSource: PushNotificationDataSource,
    private val platform: Platform
) : IPushTokenRepository {

    override fun initializeAndRegister() {
        logNetwork("RegisterPushToken", "📱 Initializing push notification manager")

        CoroutineScope(Dispatchers.Default).launch {
            pushTokenManager.getCurrentToken()?.let { token ->
                logNetwork("RegisterPushToken", "✅ Current token available: $token")
                registerToken(token)
            }
        }

        pushTokenManager.initialize { token ->
            logNetwork("RegisterPushToken", "✅ Token received, registering with backend...")

            CoroutineScope(Dispatchers.Default).launch {
                registerToken(token)
            }
        }
    }

    override suspend fun registerToken(token: String): Result<Unit> {
        logNetwork("RegisterPushToken", "📤 Sending token to backend...")

        val request = RegisterPushTokenRequest(
            token = token,
            platform = platform,
            deviceId = null
        )

        return pushNotificationDataSource.registerPushToken(request).also { result ->
            result.onSuccess {
                logNetwork("RegisterPushToken", "✅ Push token registered successfully")
            }.onFailure { error ->
                logNetwork("RegisterPushToken", "❌ Failed to register push token: ${error.message}")
            }
        }
    }

    override suspend fun deactivateAllTokens(): Result<Unit> {
        logNetwork("RegisterPushToken", "🔕 Deactivating all push tokens...")
        return pushNotificationDataSource.deactivateAllTokens()
    }
}

