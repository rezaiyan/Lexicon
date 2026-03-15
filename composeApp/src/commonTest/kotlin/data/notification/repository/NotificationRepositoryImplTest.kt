package data.notification.repository

import core.common.getOrThrow
import notification.INotificationManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationRepositoryImplTest {

    private val notificationManager = FakeNotificationManager()

    private fun createRepo() = NotificationRepositoryImpl(notificationManager)

    @Test
    fun `scheduleReviewReminder delegates to notification manager`() = runTest {
        val repo = createRepo()

        repo.scheduleReviewReminder(
            dueCount = 10,
            title = "Review Time",
            message = "You have 10 cards due",
            delayMinutes = 30
        )

        assertEquals(10, notificationManager.lastDueCount)
        assertEquals("Review Time", notificationManager.lastTitle)
        assertEquals("You have 10 cards due", notificationManager.lastMessage)
        assertEquals(30, notificationManager.lastDelayMinutes)
    }

    @Test
    fun `areNotificationsEnabled returns true when enabled`() = runTest {
        notificationManager.notificationsEnabled = true
        val repo = createRepo()

        assertTrue(repo.areNotificationsEnabled().getOrThrow())
    }

    @Test
    fun `areNotificationsEnabled returns false when disabled`() = runTest {
        notificationManager.notificationsEnabled = false
        val repo = createRepo()

        assertFalse(repo.areNotificationsEnabled().getOrThrow())
    }

    @Test
    fun `requestNotificationPermission delegates and returns result`() = runTest {
        notificationManager.permissionResult = true
        val repo = createRepo()

        assertTrue(repo.requestNotificationPermission().getOrThrow())
    }

    @Test
    fun `requestNotificationPermission returns false when denied`() = runTest {
        notificationManager.permissionResult = false
        val repo = createRepo()

        assertFalse(repo.requestNotificationPermission().getOrThrow())
    }

    @Test
    fun `wasNotificationPermissionDenied delegates to manager`() = runTest {
        notificationManager.permissionDenied = true
        val repo = createRepo()

        assertTrue(repo.wasNotificationPermissionDenied().getOrThrow())
    }

    // --- Fakes ---

    private class FakeNotificationManager : INotificationManager {
        var notificationsEnabled = true
        var permissionResult = true
        var permissionDenied = false
        var lastDueCount: Int? = null
        var lastTitle: String? = null
        var lastMessage: String? = null
        var lastDelayMinutes: Int? = null

        override suspend fun areNotificationsEnabled(): Boolean = notificationsEnabled
        override suspend fun requestNotificationPermission(): Boolean = permissionResult
        override suspend fun wasNotificationPermissionDenied(): Boolean = permissionDenied
        override suspend fun openNotificationSettings() {}
        override suspend fun scheduleReviewReminder(dueCount: Int, title: String, message: String, delayMinutes: Int) {
            lastDueCount = dueCount
            lastTitle = title
            lastMessage = message
            lastDelayMinutes = delayMinutes
        }
        override suspend fun scheduleMotivationalNotification(title: String, message: String, delayMinutes: Int) {}
        override suspend fun cancelAllNotifications() {}
        override suspend fun showImmediateNotification(title: String, message: String) {}
        override suspend fun clearBadge() {}
    }
}
