package expects

/**
 * Platform-specific network logger
 * Android: Uses android.util.Log
 * iOS/Desktop: Uses println
 */
expect fun logNetwork(tag: String, message: String)
expect fun logPlatform(tag: String, message: String)

