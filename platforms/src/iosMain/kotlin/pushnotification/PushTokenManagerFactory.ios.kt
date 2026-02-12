package pushnotification

/**
 * iOS implementation of push token manager factory
 */
actual fun createPushTokenManager(): IPushTokenManager {
    return IOSPushTokenManager()
}

