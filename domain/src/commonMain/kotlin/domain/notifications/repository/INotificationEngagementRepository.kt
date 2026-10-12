package domain.notifications.repository

import core.common.Try

interface INotificationEngagementRepository {
    /** Tells the backend the user opened a notification, so its engagement tracking resets. */
    suspend fun reportOpened(notificationLogId: Long): Try<Unit>

    /** Tells the backend the user finished a comeback rescue session. */
    suspend fun reportRescueCompleted(rescueId: Long): Try<Unit>
}
