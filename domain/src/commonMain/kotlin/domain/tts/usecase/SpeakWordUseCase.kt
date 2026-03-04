package domain.tts.usecase

import core.common.Try
import core.common.UseCase
import core.common.getOrThrow
import domain.settings.usecase.GetCurrentLanguageUseCase
import domain.tts.repository.ITtsRepository
import utils.Language

class SpeakWordUseCase(
    private val ttsRepository: ITtsRepository,
    private val getCurrentLanguageUseCase: GetCurrentLanguageUseCase
) : UseCase<SpeakWordUseCase.Params, Unit> {
    data class Params(val text: String, val languageCode: String)

    override suspend operator fun invoke(params: Params) =
        invoke(params.text, params.languageCode)

    suspend operator fun invoke(text: String, languageCode: String): Try<Unit> = Try {
        val normalized = Language.toCode(languageCode)
        val fallback = getCurrentLanguageUseCase().getOrThrow()
        val code = normalized.takeIf { it.isNotBlank() } ?: fallback.code
        println("SpeakWordUseCase: input='$languageCode' normalized='$normalized' fallback='${fallback.code}' final='$code'")

        if (!ttsRepository.isLanguageSupported(code)) {
            println("SpeakWordUseCase: language '$code' not supported, skipping")
            return@Try
        }

        if (!ttsRepository.isModelDownloaded(code)) {
            println("SpeakWordUseCase: downloading model for '$code'")
            ttsRepository.downloadModel(code).collect { }
        }

        println("SpeakWordUseCase: speaking '$text' in '$code'")
        ttsRepository.speak(text, code)
    }
}
