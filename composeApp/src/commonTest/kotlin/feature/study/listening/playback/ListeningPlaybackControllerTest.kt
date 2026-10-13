package feature.study.listening.playback

import domain.listening.model.ListeningSelection
import domain.listening.model.ListeningSettings
import domain.listening.model.ListeningStep
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ListeningPlaybackControllerTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val ttsRepository = FakeTtsRepository()
    private val analytics = FakeAnalyticsTracker()
    private val recorder = FakeListeningRecorder()
    private val pauseMs = ListeningSettings.DEFAULT_PAUSE_MS
    private val speakable = setOf("de", "en")

    @AfterTest
    fun tearDown() = scope.cancel()

    private fun word(id: Int) = Word(
        id = id,
        originalWord = "Hund$id",
        translation = "dog$id",
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.GERMAN,
        nextReviewDate = 0L,
    )

    private fun TestScope.controller() = ListeningPlaybackController(
        scope = scope,
        speakWord = SpeakWordUseCase(ttsRepository),
        stopSpeaking = StopSpeakingUseCase(ttsRepository),
        observeSettings = ObserveListeningSettingsUseCase(FakeListeningSettingsRepository()),
        observeSpeechRate = ObserveSpeechRateUseCase(FakeSettingsRepository()),
        analyticsTracker = analytics,
        recordSession = RecordListeningSessionUseCase(recorder),
        generateSessionId = GenerateSessionIdUseCase(Random(2)),
        now = { testScheduler.currentTime },
    ).also { runCurrent() }

    private fun TestScope.started(vararg ids: Int) = controller().also {
        it.start(ids.map(::word), speakable, ListeningSelection())
        runCurrent()
    }

    private val ListeningPlaybackController.active get() = playback.value as ListeningPlayback.Active

    @Test
    fun `finished session reports heard and total words`() = runTest(dispatcher) {
        val controller = started(1, 2)

        advanceUntilIdle()

        val finished = assertIs<ListeningPlayback.Finished>(controller.playback.value)
        assertEquals(2, finished.wordsHeard)
        assertEquals(2, finished.totalWords)
    }

    @Test
    fun `repeat replays the same words as a fresh recorded session`() = runTest(dispatcher) {
        val controller = started(1, 2)
        advanceUntilIdle()
        ttsRepository.spoken.clear()

        controller.repeat()
        advanceUntilIdle()

        assertEquals(
            listOf("Hund1" to "de", "dog1" to "en", "Hund2" to "de", "dog2" to "en"),
            ttsRepository.spoken,
        )
        assertEquals(2, recorder.recorded.size)
        assertEquals(2, analytics.events.count { it.first == "listening_session_start" })
    }

    @Test
    fun `close stops speech and returns to idle`() = runTest(dispatcher) {
        val controller = started(1, 2)

        controller.close()
        advanceUntilIdle()

        assertEquals(ListeningPlayback.Idle, controller.playback.value)
        assertEquals(1, ttsRepository.spoken.size)
        assertTrue(analytics.events.any { it.first == "listening_session_abandoned" })
    }

    @Test
    fun `close after finishing clears it without recording again`() = runTest(dispatcher) {
        val controller = started(1)
        advanceUntilIdle()

        controller.close()
        advanceUntilIdle()

        assertEquals(ListeningPlayback.Idle, controller.playback.value)
        assertEquals(1, recorder.recorded.size)
    }

    @Test
    fun `resume replays the current word from its prompt`() = runTest(dispatcher) {
        val controller = started(1, 2)
        advanceTimeBy(pauseMs)
        runCurrent()
        assertEquals(ListeningStep.Gap, controller.active.session.step)

        controller.pause()
        runCurrent()
        assertFalse(controller.active.isPlaying)

        controller.resume()
        runCurrent()

        assertEquals(0, controller.active.session.index)
        assertEquals("Hund1" to "de", ttsRepository.spoken.last())
    }

    @Test
    fun `previous early in a word goes back a word`() = runTest(dispatcher) {
        val controller = started(1, 2)
        controller.next()
        runCurrent()

        controller.previous()
        runCurrent()

        assertEquals(0, controller.active.session.index)
    }

    @Test
    fun `previous later in a word restarts it`() = runTest(dispatcher) {
        val controller = started(1, 2)
        controller.next()
        runCurrent()
        advanceTimeBy(ListeningPlaybackController.PREVIOUS_RESTART_THRESHOLD_MS + 1)
        runCurrent()

        ttsRepository.spoken.clear()

        controller.previous()
        runCurrent()

        assertEquals(1, controller.active.session.index)
        assertEquals(listOf("Hund2" to "de"), ttsRepository.spoken)
    }

    @Test
    fun `play and pause are idempotent`() = runTest(dispatcher) {
        val controller = started(1, 2)
        advanceTimeBy(pauseMs)
        runCurrent()

        controller.resume()
        controller.pause()
        controller.pause()
        runCurrent()

        assertFalse(controller.active.isPlaying)
        controller.close()
        advanceUntilIdle()
        assertEquals(1, recorder.recorded.single().pauseCount)
    }
}
