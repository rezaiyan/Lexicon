package pushnotification

/**
 * Factory function to create platform-specific IPushTokenManager
 * Implemented as expect/actual for each platform
 */
expect fun createPushTokenManager(): IPushTokenManager

