package domain.notifications.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.notifications.repository.INotificationRepository

/**
 * Use case to open the system notification settings screen for this app.
 * This allows users to manually enable notifications if they previously denied permission.
 */
class OpenNotificationSettingsUseCase(
    private val notificationRepository: INotificationRepository
) : NoParamUseCase<Unit> {
    override suspend operator fun invoke(params: Unit) = invoke()

    suspend operator fun invoke(): Try<Unit> =
        notificationRepository.openNotificationSettings()
}
