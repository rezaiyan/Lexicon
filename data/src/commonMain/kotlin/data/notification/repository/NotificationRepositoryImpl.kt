package data.notification.repository

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
    ) {
        notificationManager.scheduleReviewReminder(
            dueCount = dueCount,
            title = title,
            message = message,
            delayMinutes = delayMinutes
        )
    }

    override suspend fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }

    override suspend fun requestNotificationPermission(): Boolean {
        return notificationManager.requestNotificationPermission()
    }

    override suspend fun wasNotificationPermissionDenied(): Boolean {
        return notificationManager.wasNotificationPermissionDenied()
    }

    override suspend fun openNotificationSettings() {
        notificationManager.openNotificationSettings()
    }
}

