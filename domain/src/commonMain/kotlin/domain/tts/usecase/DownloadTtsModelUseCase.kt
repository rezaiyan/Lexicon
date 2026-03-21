package domain.tts.usecase

import domain.tts.repository.ITtsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Initiates download of a TTS model for the given language code,
 * emitting progress values in [0.0, 1.0].
 */
class DownloadTtsModelUseCase(
    private val ttsRepository: ITtsRepository,
) {
    suspend operator fun invoke(languageCode: String): Flow<Float> =
        ttsRepository.downloadModel(languageCode)
}
