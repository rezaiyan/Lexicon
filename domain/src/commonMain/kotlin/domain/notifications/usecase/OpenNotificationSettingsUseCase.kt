package domain.notifications.usecase

import domain.notifications.repository.INotificationRepository

/**
 * Use case to open the system notification settings screen for this app.
 * This allows users to manually enable notifications if they previously denied permission.
 */
class OpenNotificationSettingsUseCase(
    private val notificationRepository: INotificationRepository
) {
    suspend operator fun invoke() {
        notificationRepository.openNotificationSettings()
    }
}
