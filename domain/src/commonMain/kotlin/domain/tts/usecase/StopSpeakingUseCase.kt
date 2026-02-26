package domain.tts.usecase

import domain.tts.repository.ITtsRepository

class StopSpeakingUseCase(
    private val ttsRepository: ITtsRepository
) {
    suspend operator fun invoke() {
        ttsRepository.stop()
    }
}
