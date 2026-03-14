package domain.notifications.repository

import core.common.Try

interface INotificationRepository {
    suspend fun scheduleReviewReminder(
        dueCount: Int,
        title: String,
        message: String,
        delayMinutes: Int
    ): Try<Unit>
    suspend fun areNotificationsEnabled(): Try<Boolean>
    suspend fun requestNotificationPermission(): Try<Boolean>
    suspend fun wasNotificationPermissionDenied(): Try<Boolean>
    suspend fun openNotificationSettings(): Try<Unit>
}



