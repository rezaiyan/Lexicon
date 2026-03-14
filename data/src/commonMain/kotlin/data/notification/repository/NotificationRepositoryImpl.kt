package data.notification.repository

import core.common.Try
import domain.notifications.repository.INotificationRepository
import notification.INotificationManager

class NotificationRepositoryImpl(
    private val notificationManager: INotificationManager
) : INotificationRepository {

    override suspend fun scheduleReviewReminder(
        dueCount: Int,
        title: String,
        message: String,
        delayMinutes: Int
    ): Try<Unit> = Try {
        notificationManager.scheduleReviewReminder(
            dueCount = dueCount,
            title = title,
            message = message,
            delayMinutes = delayMinutes
        )
    }

    override suspend fun areNotificationsEnabled(): Try<Boolean> = Try {
        notificationManager.areNotificationsEnabled()
    }

    override suspend fun requestNotificationPermission(): Try<Boolean> = Try {
        notificationManager.requestNotificationPermission()
    }

    override suspend fun wasNotificationPermissionDenied(): Try<Boolean> = Try {
        notificationManager.wasNotificationPermissionDenied()
    }

    override suspend fun openNotificationSettings(): Try<Unit> = Try {
        notificationManager.openNotificationSettings()
    }
}

