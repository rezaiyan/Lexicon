package domain.settings.usecase

import domain.settings.repository.ISettingsRepository

/**
 * Use case to enable or disable in-app notification settings.
 * Note: This only controls the app's internal notification preference.
 * System-level permissions are handled separately.
 */
class SetNotificationsEnabledUseCase(
    private val settingsRepository: ISettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        settingsRepository.setNotificationsEnabled(enabled)
    }
}
