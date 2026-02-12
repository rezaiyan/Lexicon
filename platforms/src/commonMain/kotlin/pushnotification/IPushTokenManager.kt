package pushnotification

/**
 * Platform-specific push notification token manager
 * 
 * Handles platform-specific FCM token retrieval and initialization
 */
interface IPushTokenManager {
    /**
     * Initialize push notifications and request permissions if needed
     * 
     * @param onTokenReceived Callback when FCM token is received
     */
    fun initialize(onTokenReceived: (String) -> Unit)
    
    /**
     * Get the current FCM token if available
     * Returns null if not yet initialized
     */
    suspend fun getCurrentToken(): String?
}

