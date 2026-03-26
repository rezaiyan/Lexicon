package domain.tts.usecase

import app.cash.turbine.test
import core.common.Try
import core.common.exceptionOrNull
import core.common.getOrNull
import domain.tts.model.TtsModelInfo
import domain.tts.model.TtsState
import domain.tts.repository.ITtsRepository
import fakes.FakeSettingsRepository
import fakes.FakeTtsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TtsUseCaseTests {

    // ─────────────────────────────────────────────────────────────────────────
    // DeleteTtsModelUseCase
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `DeleteTtsModelUseCase returns success when repo deleteModel succeeds`() = runTest {
        val repo = FakeTtsRepository()
        val useCase = DeleteTtsModelUseCase(repo)

        val result = useCase("en")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `DeleteTtsModelUseCase propagates failure from repository`() = runTest {
        val repo = failingDeleteRepo()
        val useCase = DeleteTtsModelUseCase(repo)

        val result = useCase("en")

        assertTrue(result.isFailure)
        assertIs<RuntimeException>(result.exceptionOrNull())
        assertEquals("Delete failed", result.exceptionOrNull()?.message)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DownloadTtsModelUseCase
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `DownloadTtsModelUseCase returns flow from repository`() = runTest {
        val repo = FakeTtsRepository()
        val useCase = DownloadTtsModelUseCase(repo)

        val emissions = useCase("en").toList()

        assertTrue(emissions.isNotEmpty())
    }

    @Test
    fun `DownloadTtsModelUseCase flow emits 1_0f progress value`() = runTest {
        val repo = FakeTtsRepository()
        val useCase = DownloadTtsModelUseCase(repo)

        useCase("en").test {
            assertEquals(1.0f, awaitItem())
            awaitComplete()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GetTtsModelsInfoUseCase
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `GetTtsModelsInfoUseCase returns list with model info and selectedSpeakerId from settings`() = runTest {
        val repo = FakeTtsRepository() // getSupportedLanguageCodes = setOf("en")
        val settings = FakeSettingsRepository()
        val useCase = GetTtsModelsInfoUseCase(repo, settings)

        val result = useCase()

        assertTrue(result.isSuccess)
        val models = result.getOrNull()!!
        assertEquals(1, models.size)
        val model = models.first()
        assertEquals("en", model.languageCode)
        assertEquals("English", model.languageDisplayName)
        // FakeSettingsRepository.getTtsVoiceForLanguage uses default impl returning flowOf(DEFAULT_SPEAKER_ID = 0)
        assertEquals(0, model.selectedSpeakerId)
    }

    @Test
    fun `GetTtsModelsInfoUseCase returns empty list when no supported languages`() = runTest {
        val repo = stubTtsRepo(supportedCodes = emptySet())
        val settings = FakeSettingsRepository()
        val useCase = GetTtsModelsInfoUseCase(repo, settings)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(emptyList(), result.getOrNull())
    }

    @Test
    fun `GetTtsModelsInfoUseCase sorts list by languageDisplayName`() = runTest {
        val repo = stubTtsRepo(supportedCodes = setOf("de", "en", "fr"))
        val settings = FakeSettingsRepository()
        val useCase = GetTtsModelsInfoUseCase(repo, settings)

        val result = useCase()

        assertTrue(result.isSuccess)
        val names = result.getOrNull()!!.map { it.languageDisplayName }
        assertEquals(names.sorted(), names)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ObserveTtsStateUseCase
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `ObserveTtsStateUseCase returns ttsState flow from repository`() = runTest {
        val repo = FakeTtsRepository()
        val useCase = ObserveTtsStateUseCase(repo)

        useCase(Unit).test {
            assertIs<TtsState.Idle>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ObserveTtsStateUseCase state changes propagate through flow`() = runTest {
        val stateFlow = MutableStateFlow<TtsState>(TtsState.Idle)
        val repo = stubTtsRepo(ttsStateFlow = stateFlow)
        val useCase = ObserveTtsStateUseCase(repo)

        useCase(Unit).test {
            assertIs<TtsState.Idle>(awaitItem())

            stateFlow.value = TtsState.Speaking
            assertIs<TtsState.Speaking>(awaitItem())

            stateFlow.value = TtsState.Idle
            assertIs<TtsState.Idle>(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Factory helpers to avoid subclassing the final FakeTtsRepository
    // ─────────────────────────────────────────────────────────────────────────

    private fun failingDeleteRepo(): ITtsRepository = stubTtsRepo(deleteResult = Try.failure(RuntimeException("Delete failed")))

    private fun stubTtsRepo(
        supportedCodes: Set<String> = setOf("en"),
        ttsStateFlow: StateFlow<TtsState> = MutableStateFlow(TtsState.Idle),
        deleteResult: Try<Unit> = Try.success(Unit),
    ): ITtsRepository = object : ITtsRepository {
        override val ttsState: StateFlow<TtsState> = ttsStateFlow
        override suspend fun speak(text: String, languageCode: String): Try<Unit> = Try.success(Unit)
        override suspend fun stop(): Try<Unit> = Try.success(Unit)
        override suspend fun isModelDownloaded(languageCode: String): Try<Boolean> = Try.success(true)
        override suspend fun downloadModel(languageCode: String): Flow<Float> = flowOf(1.0f)
        override fun isLanguageSupported(languageCode: String): Boolean = true
        override fun getSupportedLanguageCodes(): Set<String> = supportedCodes
        override suspend fun getModelInfo(languageCode: String, displayName: String): Try<TtsModelInfo> =
            Try.success(TtsModelInfo(languageCode, displayName, false, 0L))
        override suspend fun deleteModel(languageCode: String): Try<Unit> = deleteResult
    }
}
