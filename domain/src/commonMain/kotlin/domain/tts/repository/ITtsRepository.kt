package domain.tts.repository

import core.common.Try
import domain.tts.model.TtsModelInfo
import domain.tts.model.TtsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ITtsRepository {
    val ttsState: StateFlow<TtsState>
    suspend fun speak(text: String, languageCode: String): Try<Unit>
    suspend fun stop(): Try<Unit>
    suspend fun isModelDownloaded(languageCode: String): Try<Boolean>
    suspend fun downloadModel(languageCode: String): Flow<Float>
    fun isLanguageSupported(languageCode: String): Boolean
    fun getSupportedLanguageCodes(): Set<String>
    suspend fun getModelInfo(languageCode: String, displayName: String): Try<TtsModelInfo>
    suspend fun deleteModel(languageCode: String): Try<Unit>

    /**
     * Makes [voiceId] the language's voice. A different voice already on disk is removed, so the
     * next [downloadModel] fetches the new one. Default keeps existing fakes compiling.
     */
    suspend fun selectVoice(languageCode: String, voiceId: String): Try<Unit> = Try.success(Unit)
}
