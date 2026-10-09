package fakes

import core.common.Try
import domain.tts.model.TtsModelInfo
import domain.tts.model.TtsState
import domain.tts.repository.ITtsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion

class FakeTtsRepository : ITtsRepository {
    var stopCalled = false
    var shouldThrow = false
    var speakCalled = false
    var lastSpokenText: String? = null
    var lastSpokenLanguageCode: String? = null
    var modelDownloaded = true
    var languageSupported = true
    var unsupportedLanguages: Set<String> = emptySet()
    val missingLanguages = mutableSetOf<String>()
    var downloadShouldFail = false
    val spoken = mutableListOf<Pair<String, String>>()
    var stopCount = 0

    override val ttsState: StateFlow<TtsState> = MutableStateFlow(TtsState.Idle)

    override suspend fun speak(text: String, languageCode: String): Try<Unit> {
        if (shouldThrow) return Try.failure(RuntimeException("TTS error"))
        speakCalled = true
        lastSpokenText = text
        lastSpokenLanguageCode = languageCode
        spoken += text to languageCode
        return Try.success(Unit)
    }

    override suspend fun stop(): Try<Unit> {
        if (shouldThrow) return Try.failure(RuntimeException("Stop error"))
        stopCalled = true
        stopCount++
        return Try.success(Unit)
    }

    override suspend fun isModelDownloaded(languageCode: String): Try<Boolean> =
        Try.success(modelDownloaded && languageCode !in missingLanguages)
    override suspend fun downloadModel(languageCode: String): Flow<Float> =
        if (downloadShouldFail) {
            flow { throw RuntimeException("download failed") }
        } else {
            flowOf(1.0f).onCompletion { missingLanguages.remove(languageCode) }
        }
    override fun isLanguageSupported(languageCode: String): Boolean =
        languageSupported && languageCode !in unsupportedLanguages
    override fun getSupportedLanguageCodes(): Set<String> = setOf("en")
    override suspend fun getModelInfo(languageCode: String, displayName: String): Try<TtsModelInfo> =
        Try.success(TtsModelInfo(languageCode, displayName, false, 0L))
    override suspend fun deleteModel(languageCode: String): Try<Unit> = Try.success(Unit)
}
