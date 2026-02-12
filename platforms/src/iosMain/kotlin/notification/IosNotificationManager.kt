package notification

import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
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
            
            center.addNotificationRequest(request) { error ->
                if (error != null) {
                    println("Error scheduling notification: ${error.localizedDescription}")
                }
            }
        } catch (e: Exception) {
            println("Exception scheduling notification: ${e.message}")
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
            
            center.addNotificationRequest(request) { error ->
                if (error != null) {
                    println("Error scheduling notification: ${error.localizedDescription}")
                }
            }
        } catch (e: Exception) {
            println("Exception scheduling notification: ${e.message}")
        }
    }
    
    override suspend fun cancelAllNotifications() {
        try {
            center.removeAllPendingNotificationRequests()
            center.removeAllDeliveredNotifications()
        } catch (e: Exception) {
            println("Exception canceling notifications: ${e.message}")
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
            }
            
            // Use consistent identifier so new notifications replace old ones
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = "vokab_immediate",
                content = content,
                trigger = null
            )
            
            center.addNotificationRequest(request) { error ->
                if (error != null) {
                    println("Error showing notification: ${error.localizedDescription}")
                }
            }
        } catch (e: Exception) {
            println("Exception showing notification: ${e.message}")
        }
    }
    
    override suspend fun clearBadge() {
        try {
            // Clear the app icon badge
            UIApplication.sharedApplication.setApplicationIconBadgeNumber(0)
            // Also clear delivered notifications
            center.removeAllDeliveredNotifications()
            println("✅ Badge cleared")
        } catch (e: Exception) {
            println("Exception clearing badge: ${e.message}")
        }
    }
    
    override suspend fun openNotificationSettings() {
        try {
            // Open app settings where user can enable notifications
            val settingsUrl = NSURL.URLWithString("app-settings:")
            if (settingsUrl != null && UIApplication.sharedApplication.canOpenURL(settingsUrl)) {
                UIApplication.sharedApplication.openURL(settingsUrl)
                println("🔧 Opened notification settings")
            } else {
                println("⚠️ Cannot open settings URL")
            }
        } catch (e: Exception) {
            println("⚠️ Failed to open notification settings: ${e.message}")
        }
    }
}

