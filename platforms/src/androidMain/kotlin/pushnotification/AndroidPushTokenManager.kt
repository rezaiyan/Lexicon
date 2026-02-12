package pushnotification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import pushnotification.IPushTokenManager

/**
 * Android implementation of push token manager using Firebase Cloud Messaging
 */
class AndroidPushTokenManager : IPushTokenManager {
    
    override fun initialize(onTokenReceived: (String) -> Unit) {
        // Set callback for token refresh notifications (from LexiconFirebaseMessagingService)
        setTokenRefreshCallback(onTokenReceived)
        
        // Get FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "✅ FCM Token received: ${token.take(20)}...")
                onTokenReceived(token)
            } else {
                Log.e(TAG, "❌ Failed to get FCM token", task.exception)
            }
        }
    }
    
    override suspend fun getCurrentToken(): String? {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "✅ Current FCM Token: ${token.take(20)}...")
            token
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get current FCM token", e)
            null
        }
    }
    
    companion object {
        private const val TAG = "AndroidPushToken"
        
        // Static callback holder for the Firebase Messaging Service to use
        private var tokenReceivedCallback: ((String) -> Unit)? = null
        
        /**
         * Set the callback for when a new token is received
         * Called from LexiconFirebaseMessagingService.onNewToken()
         */
        fun setTokenRefreshCallback(callback: (String) -> Unit) {
            tokenReceivedCallback = callback
        }
        
        /**
         * Notify that a new token was received
         * Called from LexiconFirebaseMessagingService.onNewToken()
         */
        fun onTokenRefreshed(token: String) {
            Log.d(TAG, "🔄 Token refreshed: ${token.take(20)}...")
            tokenReceivedCallback?.invoke(token)
        }
    }
}

