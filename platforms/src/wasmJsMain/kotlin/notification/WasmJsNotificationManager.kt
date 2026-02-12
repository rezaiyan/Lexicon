package notification

/**
 * WasmJs implementation of INotificationManager
 * No-op implementation - web notifications are not supported in this version
 */
class WasmJsNotificationManager : INotificationManager {

    override suspend fun areNotificationsEnabled(): Boolean = false

    override suspend fun requestNotificationPermission(): Boolean = false

    override suspend fun wasNotificationPermissionDenied(): Boolean = false

    override suspend fun scheduleReviewReminder(
        dueCount: Int,
        title: String,
        message: String,
        delayMinutes: Int
    ) {
        // No-op on web
    }

    override suspend fun scheduleMotivationalNotification(
        title: String,
        message: String,
        delayMinutes: Int
    ) {
        // No-op on web
    }

    override suspend fun cancelAllNotifications() {
        // No-op on web
    }

    override suspend fun showImmediateNotification(title: String, message: String) {
        // No-op on web
    }

    override suspend fun clearBadge() {
        // No-op on web
    }

    override suspend fun openNotificationSettings() {
        // No-op on web
    }
}
