package domain.tts.usecase

import core.common.Try
import core.common.UseCase
import core.common.getOrThrow
import domain.tts.repository.ITtsRepository
import utils.Language

class SpeakWordUseCase(
    private val ttsRepository: ITtsRepository,
) : UseCase<SpeakWordUseCase.Params, Unit> {
    data class Params(val text: String, val languageCode: String)

    override suspend operator fun invoke(params: Params) =
        invoke(params.text, params.languageCode)

    suspend operator fun invoke(text: String, languageCode: String): Try<Unit> = Try {
        val code = Language.toCode(languageCode.trim())
        // A word without a language can't pick a voice; speaking it in a guessed one sounds wrong.
        if (code.isBlank() || !ttsRepository.isLanguageSupported(code)) {
            return@Try
        }

        if (!ttsRepository.isModelDownloaded(code).getOrThrow()) {
            ttsRepository.downloadModel(code).collect { }
        }

        ttsRepository.speak(text, code).getOrThrow()
    }
}
