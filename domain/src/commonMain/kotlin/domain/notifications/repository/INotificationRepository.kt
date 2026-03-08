package domain.notifications.repository

interface INotificationRepository {
    suspend fun scheduleReviewReminder(
        dueCount: Int,
        title: String,
        message: String,
        delayMinutes: Int
    )
    suspend fun areNotificationsEnabled(): Boolean
    suspend fun requestNotificationPermission(): Boolean
    suspend fun wasNotificationPermissionDenied(): Boolean
    suspend fun openNotificationSettings()
}



