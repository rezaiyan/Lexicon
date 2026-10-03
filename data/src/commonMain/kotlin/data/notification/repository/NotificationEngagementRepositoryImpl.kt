package data.notification.repository

import core.common.Try
import data.notification.remote.IPushNotificationDataSource
import domain.notifications.repository.INotificationEngagementRepository

class NotificationEngagementRepositoryImpl(
    private val pushNotificationDataSource: IPushNotificationDataSource
) : INotificationEngagementRepository {

    override suspend fun reportOpened(notificationLogId: Long): Try<Unit> =
        pushNotificationDataSource.reportNotificationOpened(notificationLogId)
}
