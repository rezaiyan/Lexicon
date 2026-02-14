@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package notification

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Helper functions that use js() as single expressions (required for Kotlin/Wasm)
private fun isNotificationSupported(): Boolean =
    js("typeof window !== 'undefined' && typeof window.Notification !== 'undefined'")

private fun getNotificationPermission(): String =
    js("window.Notification ? window.Notification.permission : 'denied'")

private fun requestNotificationPermissionJs(onResult: (String) -> Unit): Unit =
    js("window.Notification.requestPermission().then(result => onResult(result))")

private fun createNotificationWithBody(title: String, body: String): Unit =
    js("new window.Notification(title, { body: body })")

private fun consoleLog(message: String): Unit =
    js("console.log(message)")

private suspend fun requestNotificationPermissionAsync(): String =
    suspendCancellableCoroutine { continuation ->
        requestNotificationPermissionJs { result ->
            continuation.resume(result)
        }
    }

/**
 * WasmJs implementation of INotificationManager
 * Uses browser Notification API for web notifications
 */
class WasmJsNotificationManager : INotificationManager {

    override suspend fun areNotificationsEnabled(): Boolean {
        if (!isNotificationSupported()) return false

        return getNotificationPermission() == "granted"
    }

    override suspend fun requestNotificationPermission(): Boolean {
        if (!isNotificationSupported()) return false

        return try {
            val result = requestNotificationPermissionAsync()
            result == "granted"
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun wasNotificationPermissionDenied(): Boolean {
        if (!isNotificationSupported()) return false

        return getNotificationPermission() == "denied"
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
                createNotificationWithBody(title, message)
            } catch (e: Exception) {
                consoleLog("Failed to show notification: ${e.message}")
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
