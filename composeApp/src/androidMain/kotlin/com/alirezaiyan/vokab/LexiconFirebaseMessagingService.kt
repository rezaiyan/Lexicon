package com.alirezaiyan.vokab

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import domain.auth.repository.IAuthRepository
import notification.NotificationData
import notification.NotificationDisplayService
import notification.NotificationHandler
import notification.UserNotificationFilter
import notification.payload.NotificationPayloadHandlerRegistry
import org.koin.android.ext.android.inject
import pushnotification.AndroidPushTokenManager

/**
 * Firebase Cloud Messaging Service
 * Handles push notifications and token refresh
 */
class LexiconFirebaseMessagingService : FirebaseMessagingService() {
    
    private val authRepository: IAuthRepository by inject()
    private val payloadHandlerRegistry: NotificationPayloadHandlerRegistry by inject()
    private val displayService: NotificationDisplayService by inject()
    
    private val notificationHandler: NotificationHandler by lazy {
        NotificationHandler(
            filter = UserNotificationFilter(),
            authRepository = authRepository
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, " New FCM token received: ${token.take(20)}...")
        
        // Notify the token manager that a new token is available
        // The token manager will then send it to the backend
        AndroidPushTokenManager.onTokenRefreshed(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        val notificationData = NotificationData.fromMap(
            title = message.notification?.title ?: "Lexicon",
            body = message.notification?.body ?: "",
            data = message.data
        )

        notificationHandler.processNotificationAsync(
            category = notificationData.category,
            onShouldShow = {
                displayService.showNotification(
                    notificationData.title,
                    notificationData.body,
                    notificationData.data
                )
                payloadHandlerRegistry.handle(notificationData.type, notificationData.body, notificationData.data)
            },
            onShouldSkip = {
                Log.d(TAG, "Skipping notification - user not authenticated. Type: ${notificationData.type}")
            }
        )
    }

    companion object {
        private const val TAG = "LexiconFCM"
    }
}