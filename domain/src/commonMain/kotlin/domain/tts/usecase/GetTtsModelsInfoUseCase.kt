package domain.tts.usecase

import core.common.Try
import core.common.getOrThrow
import domain.tts.model.TtsModelInfo
import domain.tts.repository.ITtsRepository
import utils.Language

/**
 * Returns information about all supported TTS models,
 * including download status and on-disk size.
 */
class GetTtsModelsInfoUseCase(
    private val ttsRepository: ITtsRepository,
) {
    suspend operator fun invoke(): Try<List<TtsModelInfo>> = Try {
        val supportedCodes = ttsRepository.getSupportedLanguageCodes()
        supportedCodes.map { code ->
            val language = Language.fromCode(code)
            ttsRepository.getModelInfo(code, language.displayName).getOrThrow()
        }.sortedBy { it.languageDisplayName }
    }
}
