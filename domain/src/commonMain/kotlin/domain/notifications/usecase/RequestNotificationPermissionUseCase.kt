package domain.notifications.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.notifications.repository.INotificationRepository

/**
 * Use case to request system-level notification permission from the user.
 * Returns true if permission was granted, false otherwise.
 */
class RequestNotificationPermissionUseCase(
    private val notificationRepository: INotificationRepository
) : NoParamUseCase<Boolean> {

    override suspend operator fun invoke(params: Unit) = invoke()

    suspend operator fun invoke(): Try<Boolean> = Try {
        notificationRepository.requestNotificationPermission()
    }
}
