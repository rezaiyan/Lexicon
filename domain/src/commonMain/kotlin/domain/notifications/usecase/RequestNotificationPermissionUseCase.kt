package domain.notifications.usecase

import domain.notifications.repository.INotificationRepository

/**
 * Use case to request system-level notification permission from the user.
 * Returns true if permission was granted, false otherwise.
 */
class RequestNotificationPermissionUseCase(
    private val notificationRepository: INotificationRepository
) {
    suspend operator fun invoke(): Boolean {
        return notificationRepository.requestNotificationPermission()
    }
}
