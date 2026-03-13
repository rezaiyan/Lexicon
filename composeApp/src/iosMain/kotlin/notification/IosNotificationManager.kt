package notification

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.UserNotifications.*
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * iOS implementation of notification manager using UserNotifications framework
 */
class IosNotificationManager : INotificationManager {
    
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    
    override suspend fun areNotificationsEnabled(): Boolean = suspendCoroutine { continuation ->
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val enabled = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
            continuation.resume(enabled)
        }
    }
    
    override suspend fun wasNotificationPermissionDenied(): Boolean = suspendCoroutine { continuation ->
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val denied = settings?.authorizationStatus == UNAuthorizationStatusDenied
            continuation.resume(denied)
        }
    }
    
    override suspend fun requestNotificationPermission(): Boolean = suspendCoroutine { continuation ->
        // First check current status
        center.getNotificationSettingsWithCompletionHandler { settings ->
            when (settings?.authorizationStatus) {
                UNAuthorizationStatusAuthorized -> {
                    // Already authorized
                    continuation.resume(true)
                }
                UNAuthorizationStatusNotDetermined -> {
                    // Request permission for the first time
                    center.requestAuthorizationWithOptions(
                        options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
                        completionHandler = { granted, error ->
                            continuation.resume(granted)
                        }
                    )
                }
                else -> {
                    // Permission denied or other status - open settings
                    val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
                    if (settingsUrl != null) {
                        UIApplication.sharedApplication.openURL(
                            url = settingsUrl,
                            options = emptyMap<Any?, Any>(),
                            completionHandler = null
                        )
                    }
                    continuation.resume(false)
                }
            }
        }
    }
    
    override suspend fun scheduleReviewReminder(
        dueCount: Int,
        title: String,
        message: String,
        delayMinutes: Int
    ) {
        if (!areNotificationsEnabled()) return
        
        try {
            val content = UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(message)
                setSound(UNNotificationSound.defaultSound())
                setBadge(NSNumber(int = dueCount))
                
                // Set category identifier for interactive actions
                setCategoryIdentifier(com.alirezaiyan.vokab.NotificationCategoryConstants.REVIEW_REMINDER)
                
                // Add userInfo with type and category for proper handling
                val userInfo = mapOf<Any?, Any>(
                    "type" to com.alirezaiyan.vokab.NotificationCategoryConstants.TYPE_REVIEW_REMINDER,
                    "category" to notification.NotificationCategory.USER.value
                )
                setUserInfo(userInfo)
            }
            
            // Use consistent identifier so new notifications replace old ones
            val identifier = if (delayMinutes == 0) "vokab_review_immediate" else "vokab_review_scheduled"
            
            val trigger = if (delayMinutes == 0) {
                null // Immediate notification
            } else {
                // Scheduled notification
                UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                    timeInterval = (delayMinutes * 60).toDouble(),
                    repeats = false
                )
            }
            
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = identifier,
                content = content,
                trigger = trigger
            )
            
            center.addNotificationRequest(request) { _ -> }
        } catch (_: Exception) {
            // Notification scheduling failed silently
        }
    }

    override suspend fun scheduleMotivationalNotification(
        title: String,
        message: String,
        delayMinutes: Int
    ) {
        if (!areNotificationsEnabled()) return
        
        try {
            val content = UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(message)
                setSound(UNNotificationSound.defaultSound())
                
                // Set category identifier for streak reminders
                setCategoryIdentifier(com.alirezaiyan.vokab.NotificationCategoryConstants.STREAK_REMINDER)
                
                // Add userInfo with type and category
                val userInfo = mapOf<Any?, Any>(
                    "type" to com.alirezaiyan.vokab.NotificationCategoryConstants.TYPE_STREAK_REMINDER,
                    "category" to notification.NotificationCategory.USER.value
                )
                setUserInfo(userInfo)
            }
            
            // Use consistent identifier so new notifications replace old ones
            val identifier = if (delayMinutes == 0) "vokab_motivational_immediate" else "vokab_motivational_scheduled"
            
            val trigger = if (delayMinutes == 0) {
                null
            } else {
                UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                    timeInterval = (delayMinutes * 60).toDouble(),
                    repeats = false
                )
            }
            
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = identifier,
                content = content,
                trigger = trigger
            )
            
            center.addNotificationRequest(request) { _ -> }
        } catch (_: Exception) {
            // Notification scheduling failed silently
        }
    }

    override suspend fun cancelAllNotifications() {
        try {
            center.removeAllPendingNotificationRequests()
            center.removeAllDeliveredNotifications()
        } catch (_: Exception) {
            // Cancellation failed silently
        }
    }
    
    override suspend fun showImmediateNotification(
        title: String,
        message: String
    ) {
        if (!areNotificationsEnabled()) return
        
        try {
            val content = UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(message)
                setSound(UNNotificationSound.defaultSound())
                
                // Set generic category for immediate notifications
                setCategoryIdentifier(com.alirezaiyan.vokab.NotificationCategoryConstants.GENERIC)
                
                // Add userInfo with category
                val userInfo = mapOf<Any?, Any>(
                    "category" to notification.NotificationCategory.SYSTEM.value
                )
                setUserInfo(userInfo)
            }
            
            // Use consistent identifier so new notifications replace old ones
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = "vokab_immediate",
                content = content,
                trigger = null
            )
            
            center.addNotificationRequest(request) { _ -> }
        } catch (_: Exception) {
            // Notification display failed silently
        }
    }
    
    override suspend fun clearBadge() {
        try {
            // Clear the app icon badge
            UIApplication.sharedApplication.setApplicationIconBadgeNumber(0)
            // Also clear delivered notifications
            center.removeAllDeliveredNotifications()
        } catch (_: Exception) {
            // Badge clearing failed silently
        }
    }
    
    override suspend fun openNotificationSettings() {
        try {
            // Open app settings where user can enable notifications
            val settingsUrl = NSURL.URLWithString("app-settings:")
            if (settingsUrl != null && UIApplication.sharedApplication.canOpenURL(settingsUrl)) {
                UIApplication.sharedApplication.openURL(settingsUrl)
            }
        } catch (_: Exception) {
            // Failed to open settings
        }
    }
}

