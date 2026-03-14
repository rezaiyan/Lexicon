package domain.settings.usecase

import core.common.Try
import core.common.UseCase
import domain.settings.repository.ISettingsRepository

/**
 * Use case to enable or disable in-app notification settings.
 * Note: This only controls the app's internal notification preference.
 * System-level permissions are handled separately.
 */
class SetNotificationsEnabledUseCase(
    private val settingsRepository: ISettingsRepository
) : UseCase<Boolean, Unit> {
    override suspend operator fun invoke(params: Boolean): Try<Unit> =
        settingsRepository.setNotificationsEnabled(params)
}
