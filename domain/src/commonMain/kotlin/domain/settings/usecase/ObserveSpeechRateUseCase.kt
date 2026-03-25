package domain.settings.usecase

import core.common.NoParamFlowUseCase
import domain.settings.repository.ISettingsRepository
import kotlinx.coroutines.flow.map

/** Exposes only the TTS speech rate stream — hides the full settings repository. */
class ObserveSpeechRateUseCase(
    private val settingsRepository: ISettingsRepository,
) : NoParamFlowUseCase<Float> {
    override fun invoke(params: Unit) =
        settingsRepository.getTtsSettings().map { it.speechRate }
}
