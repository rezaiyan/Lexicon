package pushnotification

/**
 * WasmJs implementation of IPushTokenManager
 * No-op implementation - push notifications are not supported on web
 */
class WasmJsPushTokenManager : IPushTokenManager {

    override fun initialize(onTokenReceived: (String) -> Unit) {
        // No-op: push tokens are not available on web
    }

    override suspend fun getCurrentToken(): String? = null
}
