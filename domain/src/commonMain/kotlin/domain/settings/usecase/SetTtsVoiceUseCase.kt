package domain.settings.usecase

import core.common.Try
import core.common.UseCase
import domain.settings.repository.ISettingsRepository

class SetTtsVoiceUseCase(
    private val settingsRepository: ISettingsRepository,
) : UseCase<SetTtsVoiceUseCase.Params, Unit> {
    data class Params(val languageCode: String, val speakerId: Int)

    override suspend operator fun invoke(params: Params): Try<Unit> =
        settingsRepository.setTtsVoiceForLanguage(params.languageCode, params.speakerId)
}
