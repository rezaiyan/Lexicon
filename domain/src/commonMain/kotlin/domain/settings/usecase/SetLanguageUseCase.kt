package domain.settings.usecase

import core.common.Try
import core.common.UseCase
import domain.settings.repository.ISettingsRepository
import utils.Language

/**
 * Use case to change the app's language setting.
 */
class SetLanguageUseCase(
    private val settingsRepository: ISettingsRepository
) : UseCase<Language, Unit> {
    override suspend operator fun invoke(params: Language): Try<Unit> = Try {
        settingsRepository.setLanguage(params)
    }
}
