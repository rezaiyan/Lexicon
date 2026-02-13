package notification

import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * WasmJs implementation of INotificationManager
 * Uses browser Notification API for web notifications
 */
class WasmJsNotificationManager : INotificationManager {

    override suspend fun areNotificationsEnabled(): Boolean {
        val notification = js("window.Notification")
        if (notification == undefined) return false

        val permission = js("Notification.permission").toString()
        return permission == "granted"
    }

    override suspend fun requestNotificationPermission(): Boolean {
        val notification = js("window.Notification")
        if (notification == undefined) return false

        return try {
            val result = (js("Notification.requestPermission()") as Promise<String>).await()
            result == "granted"
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun wasNotificationPermissionDenied(): Boolean {
        val notification = js("window.Notification")
        if (notification == undefined) return false

        val permission = js("Notification.permission").toString()
        return permission == "denied"
    }

    override suspend fun scheduleReviewReminder(
        dueCount: Int,
        title: String,
        message: String,
        delayMinutes: Int
    ) {
        // Browser notifications don't support scheduling
        // Would need a service worker for this
    }

    override suspend fun scheduleMotivationalNotification(
        title: String,
        message: String,
        delayMinutes: Int
    ) {
        // Browser notifications don't support scheduling
        // Would need a service worker for this
    }

    override suspend fun cancelAllNotifications() {
        // No-op - browser notifications auto-dismiss
    }

    override suspend fun showImmediateNotification(title: String, message: String) {
        if (areNotificationsEnabled()) {
            try {
                js("new Notification(title, { body: message })")
            } catch (e: Exception) {
                console.log("Failed to show notification: ${e.message}")
            }
        }
    }

    override suspend fun clearBadge() {
        // No-op on web
    }

    override suspend fun openNotificationSettings() {
        // Can't programmatically open browser notification settings
        // User must do this through browser UI
    }
}
