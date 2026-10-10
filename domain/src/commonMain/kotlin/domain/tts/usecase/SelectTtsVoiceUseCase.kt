package domain.tts.usecase

import core.common.Try
import core.common.UseCase
import domain.tts.repository.ITtsRepository

/** Switches a language to another voice model; the caller re-downloads if a model was installed. */
class SelectTtsVoiceUseCase(
    private val ttsRepository: ITtsRepository,
) : UseCase<SelectTtsVoiceUseCase.Params, Unit> {
    data class Params(val languageCode: String, val voiceId: String)

    override suspend operator fun invoke(params: Params): Try<Unit> =
        ttsRepository.selectVoice(params.languageCode, params.voiceId)
}
