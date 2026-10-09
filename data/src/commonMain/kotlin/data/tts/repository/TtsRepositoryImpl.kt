package data.tts.repository

import core.common.Try
import core.common.getOrDefault
import data.tts.LanguageModelMapping
import domain.settings.repository.ISettingsRepository
import domain.tts.model.TtsModelInfo
import domain.tts.model.TtsState
import domain.tts.repository.ITtsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import performance.IPerformanceTracer
import tts.IModelFileManager
import tts.ITtsEngine

/**
 * Keeps up to [MAX_LOADED_ENGINES] engines loaded, one per language, so flows that alternate
 * languages (listening mode: word, then translation) don't reload a model on every utterance.
 */
class TtsRepositoryImpl(
    private val engineFactory: () -> ITtsEngine,
    private val modelFileManager: IModelFileManager,
    private val performanceTracer: IPerformanceTracer,
    private val settingsRepository: ISettingsRepository,
) : ITtsRepository {

    private val _ttsState = MutableStateFlow<TtsState>(TtsState.Idle)
    override val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    // Insertion order doubles as LRU order: an engine is re-inserted on every use.
    private val engines = LinkedHashMap<String, ITtsEngine>()
    private val engineMutex = Mutex()

    override suspend fun speak(text: String, languageCode: String): Try<Unit> = Try {
        val engine = engineFor(languageCode) ?: return@Try

        val ttsSettings = settingsRepository.getTtsSettings().first()
        val speakerId = settingsRepository.getTtsVoiceForLanguage(languageCode).first()
        _ttsState.value = TtsState.Speaking
        engine.synthesizeAndPlay(text, ttsSettings.speechRate, speakerId)
        _ttsState.value = TtsState.Idle
    }

    override suspend fun stop(): Try<Unit> = Try {
        engines.values.toList().forEach { it.stop() }
        _ttsState.value = TtsState.Idle
    }

    /** Returns a ready engine for [languageCode], loading it (and evicting the LRU one) if needed. */
    private suspend fun engineFor(languageCode: String): ITtsEngine? = engineMutex.withLock {
        engines.remove(languageCode)?.let { loaded ->
            if (loaded.isInitialized()) {
                engines[languageCode] = loaded
                return@withLock loaded
            }
            loaded.release()
        }

        _ttsState.value = TtsState.Loading

        val modelPath = modelFileManager.getModelFilePath(languageCode)
        val tokensPath = modelFileManager.getTokensFilePath(languageCode)
        val dataDir = modelFileManager.getDataDir(languageCode)

        if (modelPath.isEmpty() || tokensPath.isEmpty()) {
            _ttsState.value = TtsState.Error("Model files not found for $languageCode")
            return@withLock null
        }

        while (engines.size >= MAX_LOADED_ENGINES) {
            val eldest = engines.keys.first()
            engines.remove(eldest)?.release()
        }

        val engine = engineFactory()
        engine.initialize(modelPath, tokensPath, dataDir)

        if (!engine.isInitialized()) {
            engine.release()
            _ttsState.value = TtsState.Error("Failed to initialize TTS engine for $languageCode")
            return@withLock null
        }

        engines[languageCode] = engine
        settingsRepository.cacheNumSpeakersForLanguage(languageCode, engine.numSpeakers())
        engine
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

    override fun getSupportedLanguageCodes(): Set<String> {
        return LanguageModelMapping.supportedLanguages
    }

    override suspend fun getModelInfo(languageCode: String, displayName: String): Try<TtsModelInfo> = Try {
        val isDownloaded = modelFileManager.isModelPresent(languageCode)
        val sizeBytes = if (isDownloaded) {
            modelFileManager.getModelDirectorySize(languageCode)
        } else {
            0L
        }
        val loadedEngine = engines[languageCode]?.takeIf { it.isInitialized() }
        val numSpeakers = if (isDownloaded && loadedEngine != null) {
            loadedEngine.numSpeakers()
        } else if (isDownloaded) {
            Try { settingsRepository.getNumSpeakersForLanguage(languageCode).first() }.getOrDefault(1)
        } else {
            LanguageModelMapping.getModelInfo(languageCode)?.numSpeakers ?: 1
        }
        TtsModelInfo(
            languageCode = languageCode,
            languageDisplayName = displayName,
            isDownloaded = isDownloaded,
            sizeBytes = sizeBytes,
            numSpeakers = numSpeakers,
        )
    }

    override suspend fun deleteModel(languageCode: String): Try<Unit> = Try {
        // If the language being deleted is loaded, release its engine
        engineMutex.withLock { engines.remove(languageCode)?.release() }
        modelFileManager.deleteModelFiles(languageCode)
    }

    private companion object {
        const val MAX_LOADED_ENGINES = 2
    }
}
