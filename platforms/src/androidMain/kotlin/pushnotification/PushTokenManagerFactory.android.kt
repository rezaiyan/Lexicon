package pushnotification

/**
 * Android implementation of push token manager factory
 */
actual fun createPushTokenManager(): IPushTokenManager {
    return AndroidPushTokenManager()
}

