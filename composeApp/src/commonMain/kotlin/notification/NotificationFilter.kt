package notification

import domain.auth.repository.IAuthRepository

interface NotificationFilter {
    suspend fun shouldShowNotification(
        category: NotificationCategory,
        authRepository: IAuthRepository
    ): Boolean
}

class UserNotificationFilter : NotificationFilter {
    override suspend fun shouldShowNotification(
        category: NotificationCategory,
        authRepository: IAuthRepository
    ): Boolean {
        return when (category) {
            NotificationCategory.USER -> authRepository.isAuthenticated()
            NotificationCategory.SYSTEM -> true
        }
    }
}

