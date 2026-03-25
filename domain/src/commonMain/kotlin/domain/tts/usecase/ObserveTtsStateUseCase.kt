package domain.tts.usecase

import core.common.NoParamFlowUseCase
import domain.tts.model.TtsState
import domain.tts.repository.ITtsRepository

/** Exposes only the TTS playback state stream — hides the full repository surface. */
class ObserveTtsStateUseCase(
    private val ttsRepository: ITtsRepository,
) : NoParamFlowUseCase<TtsState> {
    override fun invoke(params: Unit) = ttsRepository.ttsState
}
