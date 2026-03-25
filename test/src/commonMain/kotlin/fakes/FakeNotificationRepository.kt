package fakes

import core.common.Try
import domain.notifications.repository.INotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeNotificationRepository : INotificationRepository {
    var permissionGranted = true
    var shouldThrow = false
    var scheduledReminder = false
    var lastScheduledDueCount: Int? = null
    var lastScheduledTitle: String? = null
    var lastScheduledMessage: String? = null
    var lastScheduledDelayMinutes: Int? = null
    var openSettingsCalled = false

    override suspend fun requestNotificationPermission(): Try<Boolean> {
        if (shouldThrow) return Try.failure(RuntimeException("Permission error"))
        return Try.success(permissionGranted)
    }

    override suspend fun areNotificationsEnabled(): Try<Boolean> = Try.success(true)
    override suspend fun wasNotificationPermissionDenied(): Try<Boolean> = Try.success(false)

    override suspend fun scheduleReviewReminder(
        dueCount: Int,
        title: String,
        message: String,
        delayMinutes: Int,
    ): Try<Unit> {
        scheduledReminder = true
        lastScheduledDueCount = dueCount
        lastScheduledTitle = title
        lastScheduledMessage = message
        lastScheduledDelayMinutes = delayMinutes
        return Try.success(Unit)
    }

    override suspend fun openNotificationSettings(): Try<Unit> {
        if (shouldThrow) return Try.failure(RuntimeException("Settings error"))
        openSettingsCalled = true
        return Try.success(Unit)
    }
}
