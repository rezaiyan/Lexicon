package notification

/**
 * iOS implementation of NotificationDisplayService
 * On iOS, notifications are handled natively by the system through UNUserNotificationCenter
 * This is a no-op implementation as iOS handles notifications at the Swift/Objective-C level
 */
class IOSNotificationDisplayService : NotificationDisplayService {
    
    override fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        // On iOS, notifications are displayed by the system through UNUserNotificationCenter
        // The notification handling is done in iOSApp.swift
        // This method is kept for interface compatibility but doesn't need to do anything
        // iOS handles notifications natively, so no action needed here
    }
}

