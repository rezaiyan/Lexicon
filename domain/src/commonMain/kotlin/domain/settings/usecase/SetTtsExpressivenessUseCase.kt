package domain.settings.usecase

import core.common.Try
import core.common.UseCase
import domain.settings.repository.ISettingsRepository
import domain.tts.model.TtsSettings

class SetTtsExpressivenessUseCase(
    private val settingsRepository: ISettingsRepository
) : UseCase<Float, Unit> {
    override suspend operator fun invoke(params: Float): Try<Unit> =
        settingsRepository.setTtsExpressiveness(
            params.coerceIn(TtsSettings.MIN_EXPRESSIVENESS, TtsSettings.MAX_EXPRESSIVENESS)
        )
}
