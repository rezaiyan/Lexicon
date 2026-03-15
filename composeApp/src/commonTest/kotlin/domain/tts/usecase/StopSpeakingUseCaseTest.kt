package domain.tts.usecase

import core.common.Try
import domain.tts.model.TtsModelInfo
import domain.tts.model.TtsState
import domain.tts.repository.ITtsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class StopSpeakingUseCaseTest {

    private val repository = FakeTtsRepository()
    private val useCase = StopSpeakingUseCase(repository)

    @Test
    fun `stops speaking successfully`() = runTest {
        val result = useCase()

        assertTrue(result.isSuccess)
        assertTrue(repository.stopCalled)
    }

    @Test
    fun `invoke with Unit params delegates correctly`() = runTest {
        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertTrue(repository.stopCalled)
    }

    @Test
    fun `returns failure on error`() = runTest {
        repository.shouldThrow = true

        val result = useCase()

        assertTrue(result.isFailure)
    }
}

internal class FakeTtsRepository : ITtsRepository {
    var stopCalled = false
    var shouldThrow = false
    var speakCalled = false
    var lastSpokenText: String? = null
    var lastSpokenLanguageCode: String? = null
    var modelDownloaded = true
    var languageSupported = true

    override val ttsState: StateFlow<TtsState> = MutableStateFlow(TtsState.Idle)

    override suspend fun speak(text: String, languageCode: String): Try<Unit> {
        if (shouldThrow) return Try.failure(RuntimeException("TTS error"))
        speakCalled = true
        lastSpokenText = text
        lastSpokenLanguageCode = languageCode
        return Try.success(Unit)
    }

    override suspend fun stop(): Try<Unit> {
        if (shouldThrow) return Try.failure(RuntimeException("Stop error"))
        stopCalled = true
        return Try.success(Unit)
    }

    override suspend fun isModelDownloaded(languageCode: String): Try<Boolean> = Try.success(modelDownloaded)
    override suspend fun downloadModel(languageCode: String): Flow<Float> = flowOf(1.0f)
    override fun isLanguageSupported(languageCode: String): Boolean = languageSupported
    override fun getSupportedLanguageCodes(): Set<String> = setOf("en")
    override suspend fun getModelInfo(
        languageCode: String,
        displayName: String,
    ): Try<TtsModelInfo> =
        Try.success(TtsModelInfo(languageCode, displayName, false, 0L))
    override suspend fun deleteModel(languageCode: String): Try<Unit> = Try.success(Unit)
}
