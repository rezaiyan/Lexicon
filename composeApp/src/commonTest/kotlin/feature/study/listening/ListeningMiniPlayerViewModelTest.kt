package feature.study.listening

import domain.listening.model.ListeningSelection
import domain.listening.model.ListeningSettings
import domain.listening.usecase.ObserveListeningSettingsUseCase
import domain.listening.usecase.RecordListeningSessionUseCase
import domain.settings.usecase.ObserveSpeechRateUseCase
import domain.study.usecase.GenerateSessionIdUseCase
import domain.tts.usecase.SpeakWordUseCase
import domain.tts.usecase.StopSpeakingUseCase
import domain.word.model.Word
import fakes.FakeAnalyticsTracker
import fakes.FakeListeningRecorder
import fakes.FakeListeningSettingsRepository
import fakes.FakeSettingsRepository
import fakes.FakeTtsRepository
import feature.study.listening.playback.ListeningPlaybackController
import feature.study.listening.playback.ListeningPlayerVisibility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import utils.Language
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class ListeningMiniPlayerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val settings = FakeListeningSettingsRepository()
    private val visibility = ListeningPlayerVisibility()
    private val tts = FakeTtsRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun word(id: Int) = Word(
        id = id,
        originalWord = "Hund$id",
        translation = "dog$id",
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.GERMAN,
        nextReviewDate = 0L,
    )

    private fun TestScope.setup(): Pair<ListeningPlaybackController, ListeningMiniPlayerViewModel> {
        val controller = ListeningPlaybackController(
            scope = scope,
            speakWord = SpeakWordUseCase(tts),
            stopSpeaking = StopSpeakingUseCase(tts),
            observeSettings = ObserveListeningSettingsUseCase(settings),
            observeSpeechRate = ObserveSpeechRateUseCase(FakeSettingsRepository()),
            analyticsTracker = FakeAnalyticsTracker(),
            recordSession = RecordListeningSessionUseCase(FakeListeningRecorder()),
            generateSessionId = GenerateSessionIdUseCase(Random(1)),
            now = { testScheduler.currentTime },
        )
        val viewModel = ListeningMiniPlayerViewModel(controller, visibility, ObserveListeningSettingsUseCase(settings))
        runCurrent()
        return controller to viewModel
    }

    @Test
    fun `hidden while nothing plays or while the full player is open`() = runTest(dispatcher) {
        val (controller, viewModel) = setup()
        assertEquals(MiniPlayerState.Hidden, viewModel.currentState)

        visibility.setOpen(true)
        controller.start(listOf(word(1)), setOf("de", "en"), ListeningSelection())
        runCurrent()
        assertEquals(MiniPlayerState.Hidden, viewModel.currentState)
    }

    @Test
    fun `collapsed session shows the word, reveals the answer and tracks progress`() = runTest(dispatcher) {
        val (controller, viewModel) = setup()
        controller.start(listOf(word(1), word(2)), setOf("de", "en"), ListeningSelection())
        runCurrent()

        val prompt = assertIs<MiniPlayerState.Playing>(viewModel.currentState)
        assertEquals("Hund1", prompt.title)
        assertNull(prompt.subtitle)
        assertEquals(0.5f, prompt.progress)

        advanceTimeBy(ListeningSettings.DEFAULT_PAUSE_MS)
        runCurrent()
        assertEquals("dog1", (viewModel.currentState as MiniPlayerState.Playing).subtitle)

        viewModel.togglePlayback()
        runCurrent()
        assertFalse((viewModel.currentState as MiniPlayerState.Playing).isPlaying)
    }

    @Test
    fun `finished session offers repeat, close hides it`() = runTest(dispatcher) {
        val (controller, viewModel) = setup()
        controller.start(listOf(word(1)), setOf("de", "en"), ListeningSelection())
        advanceUntilIdle()
        assertEquals(MiniPlayerState.Finished(wordsHeard = 1), viewModel.currentState)

        viewModel.repeat()
        runCurrent()
        assertIs<MiniPlayerState.Playing>(viewModel.currentState)

        viewModel.close()
        runCurrent()
        assertEquals(MiniPlayerState.Hidden, viewModel.currentState)
    }
}
