package domain.settings.usecase

import core.common.NoParamUseCase
import core.common.Try
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
) : NoParamUseCase<Language> {

    override suspend operator fun invoke(params: Unit) = invoke()

    suspend operator fun invoke(): Try<Language> = Try {
        settingsRepository.getLanguage().first()
    }
}
