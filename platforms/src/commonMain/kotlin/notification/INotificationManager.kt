package notification

/**
 * Platform-agnostic notification manager interface
 */
interface INotificationManager {
    
    /**
     * Check if notifications are enabled at system level
     */
    suspend fun areNotificationsEnabled(): Boolean
    
    /**
     * Request notification permission (shows system dialog on Android 13+)
     */
    suspend fun requestNotificationPermission(): Boolean
    
    /**
     * Check if notification permission was previously denied by the user
     */
    suspend fun wasNotificationPermissionDenied(): Boolean
    
    /**
     * Schedule a friendly reminder notification
     */
    suspend fun scheduleReviewReminder(
        dueCount: Int,
        title: String,
        message: String,
        delayMinutes: Int = 60 // Default 1 hour from now
    )
    
    /**
     * Schedule a motivational notification
     */
    suspend fun scheduleMotivationalNotification(
        title: String,
        message: String,
        delayMinutes: Int
    )
    
    /**
     * Cancel all pending notifications
     */
    suspend fun cancelAllNotifications()
    
    /**
     * Show immediate notification (for testing or urgent reminders)
     */
    suspend fun showImmediateNotification(
        title: String,
        message: String
    )
    
    /**
     * Clear the app icon badge
     */
    suspend fun clearBadge()

    /**
     * Open system settings for app notifications
     */
    suspend fun openNotificationSettings()
}

/**
 * Notification preferences
 */
data class NotificationSettings(
    val enabled: Boolean = true,
    val reviewReminders: Boolean = true,
    val motivationalMessages: Boolean = true,
    val dailyReminderTime: String = "18:00", // 6 PM default
    val minimumDueCards: Int = 5 // Don't notify for less than 5 cards
)

