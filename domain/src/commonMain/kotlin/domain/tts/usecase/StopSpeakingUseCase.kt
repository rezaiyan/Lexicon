package domain.tts.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.tts.repository.ITtsRepository

class StopSpeakingUseCase(
    private val ttsRepository: ITtsRepository
) : NoParamUseCase<Unit> {
    override suspend operator fun invoke(params: Unit) = invoke()

    suspend operator fun invoke(): Try<Unit> =
        ttsRepository.stop()
}
