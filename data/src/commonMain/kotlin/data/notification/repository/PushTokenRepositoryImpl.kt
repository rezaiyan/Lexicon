package data.notification.repository

import data.notification.remote.IPushNotificationDataSource
import data.notification.remote.model.Platform
import data.notification.remote.model.RegisterPushTokenRequest
import data.storage.SecureStorage
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
    private val secureStorage: SecureStorage,
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
                secureStorage.savePushToken(token)
            }.onFailure { error ->
                logNetwork("RegisterPushToken", " Failed to register push token: ${error.message}")
            }
        }
    }

    override suspend fun deactivateAllTokens(): Try<Unit> {
        logNetwork("RegisterPushToken", " Deactivating all push tokens...")
        return pushNotificationDataSource.deactivateAllTokens().also { result ->
            result.onSuccess { secureStorage.clearPushToken() }
        }
    }

    override suspend fun deactivateCurrentToken(): Try<Unit> {
        // Prefer the token this device actually registered with the backend — the platform SDK's
        // live token (pushTokenManager.getCurrentToken()) can differ if it rotated since registration,
        // or be unavailable yet (e.g. iOS before the FCM delegate callback fires on a fresh process).
        val token = secureStorage.getPushToken() ?: pushTokenManager.getCurrentToken() ?: run {
            logNetwork("RegisterPushToken", " No current token to deactivate")
            return Try.success(Unit)
        }
        logNetwork("RegisterPushToken", " Deactivating current device push token...")
        return pushNotificationDataSource.deactivateToken(token).also { result ->
            result.onSuccess { secureStorage.clearPushToken() }
        }
    }
}

