package domain.tts.repository

import domain.tts.model.TtsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ITtsRepository {
    val ttsState: StateFlow<TtsState>
    suspend fun speak(text: String, languageCode: String)
    suspend fun stop()
    suspend fun isModelDownloaded(languageCode: String): Boolean
    suspend fun downloadModel(languageCode: String): Flow<Float>
    fun isLanguageSupported(languageCode: String): Boolean
}
