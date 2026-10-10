package data.tts.repository

import core.common.Try
import core.common.getOrDefault
import core.common.getOrThrow
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

    // Noise scale each loaded engine was built with; a settings change forces a reload.
    private val engineNoiseScales = mutableMapOf<String, Float>()

    override suspend fun speak(text: String, languageCode: String): Try<Unit> = Try {
        val ttsSettings = settingsRepository.getTtsSettings().first()
        val engine = engineFor(languageCode, ttsSettings.expressiveness) ?: return@Try

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
    private suspend fun engineFor(languageCode: String, noiseScale: Float): ITtsEngine? = engineMutex.withLock {
        engines.remove(languageCode)?.let { loaded ->
            if (loaded.isInitialized() && engineNoiseScales[languageCode] == noiseScale) {
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
            engineNoiseScales.remove(eldest)
        }

        val engine = engineFactory()
        engine.initialize(modelPath, tokensPath, dataDir, noiseScale)

        if (!engine.isInitialized()) {
            engine.release()
            _ttsState.value = TtsState.Error("Failed to initialize TTS engine for $languageCode")
            return@withLock null
        }

        engines[languageCode] = engine
        engineNoiseScales[languageCode] = noiseScale
        settingsRepository.cacheNumSpeakersForLanguage(languageCode, engine.numSpeakers())
        engine
    }

    override suspend fun isModelDownloaded(languageCode: String): Try<Boolean> = Try {
        modelFileManager.isModelPresent(languageCode)
    }

    override suspend fun downloadModel(languageCode: String): Flow<Float> {
        val voiceId = settingsRepository.getTtsVoiceIdForLanguage(languageCode).first()
        val modelInfo = LanguageModelMapping.getModelInfo(languageCode, voiceId)
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
        val voiceId = settingsRepository.getTtsVoiceIdForLanguage(languageCode).first()
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
            LanguageModelMapping.getModelInfo(languageCode, voiceId)?.numSpeakers ?: 1
        }
        TtsModelInfo(
            languageCode = languageCode,
            languageDisplayName = displayName,
            isDownloaded = isDownloaded,
            sizeBytes = sizeBytes,
            numSpeakers = numSpeakers,
            voices = LanguageModelMapping.getVoices(languageCode),
            selectedVoiceId = LanguageModelMapping.getModelInfo(languageCode, voiceId)?.voiceId,
            sampleText = LanguageModelMapping.getSampleText(languageCode),
        )
    }

    override suspend fun deleteModel(languageCode: String): Try<Unit> = Try {
        // If the language being deleted is loaded, release its engine
        engineMutex.withLock { engines.remove(languageCode)?.release() }
        modelFileManager.deleteModelFiles(languageCode)
    }

    override suspend fun selectVoice(languageCode: String, voiceId: String): Try<Unit> = Try {
        val requested = requireNotNull(LanguageModelMapping.getModelInfo(languageCode, voiceId)) {
            "No TTS voices for $languageCode"
        }
        require(requested.voiceId == voiceId) { "Unknown voice $voiceId for $languageCode" }

        val currentId = settingsRepository.getTtsVoiceIdForLanguage(languageCode).first()
        val current = LanguageModelMapping.getModelInfo(languageCode, currentId)
        if (current?.voiceId == voiceId) return@Try

        settingsRepository.setTtsVoiceIdForLanguage(languageCode, voiceId).getOrThrow()
        // One voice per language on disk: drop the old model so the next download fetches the new one.
        engineMutex.withLock {
            engines.remove(languageCode)?.release()
            engineNoiseScales.remove(languageCode)
        }
        modelFileManager.deleteModelFiles(languageCode)
    }

    private companion object {
        const val MAX_LOADED_ENGINES = 2
    }
}
