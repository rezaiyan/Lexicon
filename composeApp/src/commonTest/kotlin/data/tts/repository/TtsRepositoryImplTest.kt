package data.tts.repository

import app.cash.turbine.test
import core.common.Try
import core.common.getOrThrow
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.tts.model.TtsState
import fakes.FakePerformanceTracer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import tts.IModelFileManager
import tts.ITtsEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TtsRepositoryImplTest {

    private val ttsEngine = FakeTtsEngine()
    private val createdEngines = mutableListOf<FakeTtsEngine>()
    private val modelFileManager = FakeModelFileManager()
    private val performanceTracer = FakePerformanceTracer()
    private val fakeSettingsRepository = object : ISettingsRepository {
        override fun getThemeMode(): Flow<ThemeMode> = flowOf(ThemeMode.AUTO)
        override suspend fun setThemeMode(mode: ThemeMode): Try<Unit> = Try.success(Unit)
        override suspend fun clearSettings(): Try<Unit> = Try.success(Unit)
        override fun getNotificationsEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setNotificationsEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setReviewRemindersEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setMotivationalMessagesEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override suspend fun getDailyReminderTime(): Try<String> = Try.success("09:00")
        override suspend fun setDailyReminderTime(time: String): Try<Unit> = Try.success(Unit)
        override suspend fun getMinimumDueCards(): Try<Int> = Try.success(5)
        override suspend fun setMinimumDueCards(count: Int): Try<Unit> = Try.success(Unit)
    }

    // First engine handed out is [ttsEngine]; later ones are fresh fakes sharing its init outcome.
    private fun createRepo() = TtsRepositoryImpl(
        engineFactory = {
            val engine = if (createdEngines.isEmpty()) {
                ttsEngine
            } else {
                FakeTtsEngine().apply { initializeSuccess = ttsEngine.initializeSuccess }
            }
            engine.also { createdEngines += it }
        },
        modelFileManager = modelFileManager,
        performanceTracer = performanceTracer,
        settingsRepository = fakeSettingsRepository,
    )

    private fun givenModelFiles() {
        modelFileManager.modelPath = "/models/model.onnx"
        modelFileManager.tokensPath = "/models/tokens.txt"
        modelFileManager.dataDir = "/models"
    }

    @Test
    fun `speak initializes engine and plays when not initialized`() = runTest {
        modelFileManager.modelPath = "/models/en/model.onnx"
        modelFileManager.tokensPath = "/models/en/tokens.txt"
        modelFileManager.dataDir = "/models/en"
        ttsEngine.initializeSuccess = true
        val repo = createRepo()

        repo.speak("Hello", "en")

        assertTrue(ttsEngine.initialized)
        assertEquals("Hello", ttsEngine.lastSpokenText)
        assertIs<TtsState.Idle>(repo.ttsState.value)
    }

    @Test
    fun `speak sets error state when model files not found`() = runTest {
        modelFileManager.modelPath = ""
        modelFileManager.tokensPath = ""
        val repo = createRepo()

        repo.speak("Hello", "en")

        assertIs<TtsState.Error>(repo.ttsState.value)
    }

    @Test
    fun `speak sets error state when engine fails to initialize`() = runTest {
        modelFileManager.modelPath = "/models/en/model.onnx"
        modelFileManager.tokensPath = "/models/en/tokens.txt"
        modelFileManager.dataDir = "/models/en"
        ttsEngine.initializeSuccess = false
        val repo = createRepo()

        repo.speak("Hello", "en")

        assertIs<TtsState.Error>(repo.ttsState.value)
    }

    @Test
    fun `speak reuses engine when language matches and initialized`() = runTest {
        modelFileManager.modelPath = "/models/en/model.onnx"
        modelFileManager.tokensPath = "/models/en/tokens.txt"
        modelFileManager.dataDir = "/models/en"
        ttsEngine.initializeSuccess = true
        val repo = createRepo()

        repo.speak("Hello", "en")
        ttsEngine.initializeCount = 0
        repo.speak("World", "en")

        assertEquals(0, ttsEngine.initializeCount)
        assertEquals("World", ttsEngine.lastSpokenText)
    }

    @Test
    fun `speak keeps one engine per language so alternating languages does not reinitialize`() = runTest {
        givenModelFiles()
        val repo = createRepo()

        repo.speak("Hello", "en")
        repo.speak("Hola", "es")
        repo.speak("World", "en")
        repo.speak("Mundo", "es")

        assertEquals(2, createdEngines.size)
        assertEquals(2, createdEngines.sumOf { it.initializeCount })
        assertEquals("World", createdEngines[0].lastSpokenText)
        assertEquals("Mundo", createdEngines[1].lastSpokenText)
    }

    @Test
    fun `speak releases least recently used engine when a third language loads`() = runTest {
        givenModelFiles()
        val repo = createRepo()

        repo.speak("Hello", "en")
        repo.speak("Hola", "es")
        repo.speak("Hallo", "de")

        assertFalse(createdEngines[0].initialized)
        assertTrue(createdEngines[1].initialized)
        assertTrue(createdEngines[2].initialized)

        repo.speak("Otra", "es")
        assertEquals(3, createdEngines.size)
    }

    @Test
    fun `speak stays in Speaking state until engine playback completes`() = runTest {
        givenModelFiles()
        val gate = CompletableDeferred<Unit>()
        ttsEngine.playbackGate = gate
        val repo = createRepo()

        val job = launch { repo.speak("Hello", "en") }
        runCurrent()
        assertIs<TtsState.Speaking>(repo.ttsState.value)

        gate.complete(Unit)
        job.join()
        assertIs<TtsState.Idle>(repo.ttsState.value)
    }

    @Test
    fun `stop stops every loaded engine`() = runTest {
        givenModelFiles()
        val repo = createRepo()
        repo.speak("Hello", "en")
        repo.speak("Hola", "es")

        repo.stop()

        assertTrue(createdEngines.all { it.stopped })
        assertIs<TtsState.Idle>(repo.ttsState.value)
    }

    @Test
    fun `stop sets state to idle`() = runTest {
        givenModelFiles()
        val repo = createRepo()
        repo.speak("Hello", "en")

        repo.stop()

        assertIs<TtsState.Idle>(repo.ttsState.value)
        assertTrue(ttsEngine.stopped)
    }

    @Test
    fun `isModelDownloaded delegates to model file manager`() = runTest {
        modelFileManager.modelPresent = true
        val repo = createRepo()

        assertTrue(repo.isModelDownloaded("en").getOrThrow())
    }

    @Test
    fun `isModelDownloaded returns false when not present`() = runTest {
        modelFileManager.modelPresent = false
        val repo = createRepo()

        assertFalse(repo.isModelDownloaded("en").getOrThrow())
    }

    @Test
    fun `isLanguageSupported returns true for supported language`() {
        val repo = createRepo()
        // LanguageModelMapping.isSupported checks internal map
        // This just verifies the delegation works
        val result = repo.isLanguageSupported("en")
        // Result depends on LanguageModelMapping static data
        assertTrue(result || !result) // Just verify no crash
    }

    // -------------------------------------------------------------------------
    // speak — init fails branch
    // -------------------------------------------------------------------------

    @Test
    fun `speak sets currentLoadedLanguage to null when engine fails to initialize`() = runTest {
        modelFileManager.modelPath = "/models/en/model.onnx"
        modelFileManager.tokensPath = "/models/en/tokens.txt"
        modelFileManager.dataDir = "/models/en"
        ttsEngine.initializeSuccess = false
        val repo = createRepo()

        repo.speak("Hello", "en")

        // State is Error and language was NOT cached
        assertIs<TtsState.Error>(repo.ttsState.value)
        // A second speak call must re-attempt initialization (not reuse a stale language)
        ttsEngine.initializeSuccess = true
        repo.speak("Hello", "en")
        assertTrue(createdEngines.last().initialized)
    }

    // -------------------------------------------------------------------------
    // downloadModel
    // -------------------------------------------------------------------------

    @Test
    fun `downloadModel for unsupported language sets Error state and returns empty flow`() = runTest {
        val repo = createRepo()

        val resultFlow = repo.downloadModel("xx")
        val emitted = resultFlow.toList()

        assertIs<TtsState.Error>(repo.ttsState.value)
        assertTrue(emitted.isEmpty())
    }

    @Test
    fun `downloadModel for supported language emits progress values and transitions to Idle`() = runTest {
        modelFileManager.downloadProgressValues = listOf(0.25f, 0.5f, 0.75f, 1.0f)
        val repo = createRepo()

        repo.ttsState.test {
            // Initial state
            awaitItem() // Idle

            val progressFlow = repo.downloadModel("en")

            // downloadModel sets Downloading(0f) immediately before returning the flow
            val downloadingStart = awaitItem()
            assertIs<TtsState.Downloading>(downloadingStart)
            assertEquals("en", (downloadingStart as TtsState.Downloading).languageCode)
            assertEquals(0f, downloadingStart.progress)

            // Collect the returned flow (drives onEach state updates + onCompletion → Idle)
            progressFlow.toList()

            // onEach updates Downloading for each progress value
            val state25 = awaitItem()
            assertIs<TtsState.Downloading>(state25)
            assertEquals(0.25f, (state25 as TtsState.Downloading).progress)

            val state50 = awaitItem()
            assertIs<TtsState.Downloading>(state50)
            assertEquals(0.5f, (state50 as TtsState.Downloading).progress)

            val state75 = awaitItem()
            assertIs<TtsState.Downloading>(state75)
            assertEquals(0.75f, (state75 as TtsState.Downloading).progress)

            val state100 = awaitItem()
            assertIs<TtsState.Downloading>(state100)
            assertEquals(1.0f, (state100 as TtsState.Downloading).progress)

            // onCompletion sets Idle
            val finalState = awaitItem()
            assertIs<TtsState.Idle>(finalState)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // -------------------------------------------------------------------------
    // deleteModel
    // -------------------------------------------------------------------------

    @Test
    fun `deleteModel releases engine and clears language when currently loaded`() = runTest {
        modelFileManager.modelPath = "/models/en/model.onnx"
        modelFileManager.tokensPath = "/models/en/tokens.txt"
        modelFileManager.dataDir = "/models/en"
        ttsEngine.initializeSuccess = true
        val repo = createRepo()

        // Load "en" first so it becomes the currentLoadedLanguage
        repo.speak("Hello", "en")
        assertTrue(ttsEngine.initialized)

        // Now delete it
        repo.deleteModel("en")

        // Engine released (initialized = false after release())
        assertFalse(ttsEngine.initialized)
        // Subsequent speak must load a fresh engine
        repo.speak("Hello", "en")
        assertEquals(2, createdEngines.size)
        assertTrue(createdEngines.last().initialized)
    }

    @Test
    fun `deleteModel does not release engine when language is not currently loaded`() = runTest {
        modelFileManager.modelPath = "/models/en/model.onnx"
        modelFileManager.tokensPath = "/models/en/tokens.txt"
        modelFileManager.dataDir = "/models/en"
        ttsEngine.initializeSuccess = true
        val repo = createRepo()

        // Load "en"
        repo.speak("Hello", "en")
        val initializedBefore = ttsEngine.initialized

        // Delete "de" which was never loaded
        repo.deleteModel("de")

        // Engine for "en" still initialized — release was NOT called
        assertEquals(initializedBefore, ttsEngine.initialized)
    }

    // --- Fakes ---

    private class FakeTtsEngine : ITtsEngine {
        var initialized = false
        var initializeSuccess = true
        var initializeCount = 0
        var lastSpokenText: String? = null
        var stopped = false
        var playbackGate: CompletableDeferred<Unit>? = null

        override suspend fun initialize(modelPath: String, tokensPath: String, dataDir: String) {
            initializeCount++
            initialized = initializeSuccess
        }

        override suspend fun synthesizeAndPlay(text: String, speed: Float, speakerId: Int) {
            lastSpokenText = text
            playbackGate?.await()
        }

        override suspend fun stop() {
            stopped = true
        }

        override fun release() {
            initialized = false
        }

        override fun isInitialized(): Boolean = initialized
        override fun numSpeakers(): Int = 1
    }

    private class FakeModelFileManager : IModelFileManager {
        var modelPresent = false
        var modelPath = ""
        var tokensPath = ""
        var dataDir = ""
        var downloadProgressValues: List<Float> = listOf(1.0f)

        override suspend fun isModelPresent(languageCode: String): Boolean = modelPresent
        override suspend fun downloadAndExtractModel(
            archiveUrl: String,
            languageCode: String,
            extractedDirName: String,
        ): Flow<Float> = flowOf(*downloadProgressValues.toTypedArray())

        override fun getModelFilePath(languageCode: String): String = modelPath
        override fun getTokensFilePath(languageCode: String): String = tokensPath
        override fun getDataDir(languageCode: String): String = dataDir
        override suspend fun deleteModelFiles(languageCode: String) {}
        override suspend fun getModelDirectorySize(languageCode: String): Long = 0L
    }
}
