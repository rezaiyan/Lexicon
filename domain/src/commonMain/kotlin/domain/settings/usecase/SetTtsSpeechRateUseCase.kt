package domain.settings.usecase

import core.common.Try
import core.common.UseCase
import domain.settings.repository.ISettingsRepository

class SetTtsSpeechRateUseCase(
    private val settingsRepository: ISettingsRepository
) : UseCase<Float, Unit> {
    override suspend operator fun invoke(params: Float): Try<Unit> =
        settingsRepository.setTtsSpeechRate(params)
}
