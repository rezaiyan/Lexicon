package domain.settings.usecase

import domain.settings.repository.ISettingsRepository
import kotlinx.coroutines.flow.first
import utils.Language

/**
 * Use case to get the current app language setting
 * 
 * Single source of truth for language configuration across the app.
 */
class GetCurrentLanguageUseCase(
    private val settingsRepository: ISettingsRepository
) {
    suspend operator fun invoke(): Language {
        return try {
            settingsRepository.getLanguage().first()
        } catch (e: Exception) {
            Language.ENGLISH // Default fallback
        }
    }
}

