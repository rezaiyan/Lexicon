package domain.tts.usecase

import core.common.Try
import domain.tts.repository.ITtsRepository

/**
 * Deletes downloaded TTS model files for a given language.
 */
class DeleteTtsModelUseCase(
    private val ttsRepository: ITtsRepository,
) {
    suspend operator fun invoke(languageCode: String): Try<Unit> =
        ttsRepository.deleteModel(languageCode)
}
