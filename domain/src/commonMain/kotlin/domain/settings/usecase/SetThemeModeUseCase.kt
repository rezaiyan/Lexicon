package domain.settings.usecase

import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository

/**
 * Use case to change the app's theme mode (light/dark/system).
 */
class SetThemeModeUseCase(
    private val settingsRepository: ISettingsRepository
) {
    suspend operator fun invoke(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }
}
