package domain.settings.usecase

import domain.settings.repository.ISettingsRepository
import utils.Language

/**
 * Use case to change the app's language setting.
 */
class SetLanguageUseCase(
    private val settingsRepository: ISettingsRepository
) {
    suspend operator fun invoke(language: Language) {
        settingsRepository.setLanguage(language)
    }
}
