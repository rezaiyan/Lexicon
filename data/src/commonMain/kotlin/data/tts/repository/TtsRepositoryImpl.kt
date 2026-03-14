package data.tts.repository

import core.common.Try
import data.tts.LanguageModelMapping
import domain.tts.model.TtsState
import domain.tts.repository.ITtsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import performance.IPerformanceTracer
import tts.IModelFileManager
import tts.ITtsEngine

class TtsRepositoryImpl(
    private val ttsEngine: ITtsEngine,
    private val modelFileManager: IModelFileManager,
    private val performanceTracer: IPerformanceTracer,
) : ITtsRepository {

    private val _ttsState = MutableStateFlow<TtsState>(TtsState.Idle)
    override val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    private var currentLoadedLanguage: String? = null

    override suspend fun speak(text: String, languageCode: String): Try<Unit> = Try {
        if (currentLoadedLanguage != languageCode || !ttsEngine.isInitialized()) {
            _ttsState.value = TtsState.Loading

            val modelPath = modelFileManager.getModelFilePath(languageCode)
            val tokensPath = modelFileManager.getTokensFilePath(languageCode)
            val dataDir = modelFileManager.getDataDir(languageCode)

            if (modelPath.isEmpty() || tokensPath.isEmpty()) {
                _ttsState.value = TtsState.Error("Model files not found for $languageCode")
                return@Try
            }

            ttsEngine.release()
            ttsEngine.initialize(modelPath, tokensPath, dataDir)

            if (!ttsEngine.isInitialized()) {
                _ttsState.value = TtsState.Error("Failed to initialize TTS engine for $languageCode")
                currentLoadedLanguage = null
                return@Try
            }

            currentLoadedLanguage = languageCode
        }

        _ttsState.value = TtsState.Speaking
        ttsEngine.synthesizeAndPlay(text)
        _ttsState.value = TtsState.Idle
    }

    override suspend fun stop(): Try<Unit> = Try {
        ttsEngine.stop()
        _ttsState.value = TtsState.Idle
    }

    override suspend fun isModelDownloaded(languageCode: String): Try<Boolean> = Try {
        modelFileManager.isModelPresent(languageCode)
    }

    override suspend fun downloadModel(languageCode: String): Flow<Float> {
        val modelInfo = LanguageModelMapping.getModelInfo(languageCode)
        if (modelInfo == null) {
            _ttsState.value = TtsState.Error("No model available for $languageCode")
            return flow {}
        }

        _ttsState.value = TtsState.Downloading(languageCode, 0f)
        val trace = performanceTracer.startTrace("tts_model_download")
        performanceTracer.putAttribute(trace, "language", languageCode)

        return modelFileManager.downloadAndExtractModel(
            archiveUrl = modelInfo.archiveUrl,
            languageCode = languageCode,
            extractedDirName = modelInfo.extractedDirName
        ).onEach { progress ->
            _ttsState.value = TtsState.Downloading(languageCode, progress)
        }.onCompletion {
            performanceTracer.stopTrace(trace)
            _ttsState.value = TtsState.Idle
        }
    }

    override fun isLanguageSupported(languageCode: String): Boolean {
        return LanguageModelMapping.isSupported(languageCode)
    }
}
