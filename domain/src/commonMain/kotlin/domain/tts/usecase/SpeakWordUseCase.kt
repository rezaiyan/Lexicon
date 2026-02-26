package domain.tts.usecase

import domain.settings.usecase.GetCurrentLanguageUseCase
import domain.tts.repository.ITtsRepository
import utils.Language

class SpeakWordUseCase(
    private val ttsRepository: ITtsRepository,
    private val getCurrentLanguageUseCase: GetCurrentLanguageUseCase
) {
    suspend operator fun invoke(text: String, languageCode: String) {
        val normalized = Language.toCode(languageCode)
        val fallback = getCurrentLanguageUseCase()
        val code = normalized.takeIf { it.isNotBlank() } ?: fallback.code
        println("SpeakWordUseCase: input='$languageCode' normalized='$normalized' fallback='${fallback.code}' final='$code'")

        if (!ttsRepository.isLanguageSupported(code)) {
            println("SpeakWordUseCase: language '$code' not supported, skipping")
            return
        }

        if (!ttsRepository.isModelDownloaded(code)) {
            println("SpeakWordUseCase: downloading model for '$code'")
            ttsRepository.downloadModel(code).collect { }
        }

        println("SpeakWordUseCase: speaking '$text' in '$code'")
        ttsRepository.speak(text, code)
    }
}
