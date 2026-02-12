package pushnotification

import platform.Foundation.NSLog

/**
 * iOS implementation of push token manager
 * 
 * NOTE: The actual FCM token retrieval is handled in iOSApp.swift 
 * through the MessagingDelegate. This class provides the interface
 * to receive and manage those tokens from Swift.
 */
class IOSPushTokenManager : IPushTokenManager {
    
    private var onTokenReceivedCallback: ((String) -> Unit)? = null
    
    override fun initialize(onTokenReceived: (String) -> Unit) {
        NSLog("📱 [IOSPushToken] Initializing push token manager")
        this.onTokenReceivedCallback = onTokenReceived
        tokenReceivedCallback = onTokenReceived
        
        // If token was already received before initialization, trigger callback immediately
        currentToken?.let { token ->
            NSLog("📱 [IOSPushToken] Token already available, triggering callback immediately")
            onTokenReceived(token)
        } ?: run {
            NSLog("📱 [IOSPushToken] Waiting for FCM token from Swift side...")
        }
    }
    
    override suspend fun getCurrentToken(): String? {
        NSLog("📱 [IOSPushToken] getCurrentToken() - returning cached token")
        // Return the last received token
        return currentToken
    }
    
    companion object {
        // Static callback holder for Swift to call
        private var tokenReceivedCallback: ((String) -> Unit)? = null
        private var currentToken: String? = null
        
        /**
         * Called from Swift when FCM token is received
         * This is the bridge function that Swift code will invoke
         */
        fun notifyTokenReceived(token: String) {
            NSLog("📱 [IOSPushToken] ✅ Token received from Swift: $token")
            currentToken = token
            tokenReceivedCallback?.invoke(token)
        }
    }
}

