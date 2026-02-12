package pushnotification

/**
 * WasmJs factory function for push token manager
 */
actual fun createPushTokenManager(): IPushTokenManager {
    return WasmJsPushTokenManager()
}
