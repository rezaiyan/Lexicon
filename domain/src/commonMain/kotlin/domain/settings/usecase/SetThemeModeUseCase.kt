package domain.settings.usecase

import core.common.Try
import core.common.UseCase
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository

/**
 * Use case to change the app's theme mode (light/dark/system).
 */
class SetThemeModeUseCase(
    private val settingsRepository: ISettingsRepository
) : UseCase<ThemeMode, Unit> {
    override suspend operator fun invoke(params: ThemeMode): Try<Unit> = Try {
        settingsRepository.setThemeMode(params)
    }
}
