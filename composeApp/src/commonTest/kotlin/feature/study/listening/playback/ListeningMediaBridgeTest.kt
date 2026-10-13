package feature.study.listening.playback

import domain.listening.model.ListeningOrder
import domain.listening.model.ListeningSelection
import domain.listening.model.ListeningSession
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
import fakes.FakeListeningMediaSession
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
import media.ListeningMediaState
import media.MediaCommand
import media.MediaLabels
import utils.Language
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListeningMediaBridgeTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val tts = FakeTtsRepository()
    private val media = FakeListeningMediaSession()
    private val settings = FakeListeningSettingsRepository()
    private val pauseMs = ListeningSettings.DEFAULT_PAUSE_MS

    private val text = ListeningMediaText(
        labels = MediaLabels(stop = "Stop", repeat = "Repeat", close = "Close"),
        position = { current, total -> "$current of $total" },
        finishedTitle = "Nice listening!",
        finishedSubtitle = { heard, minutes -> "$heard words · $minutes min" },
    )

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

    private fun TestScope.started(vararg ids: Int): ListeningPlaybackController {
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
        ListeningMediaBridge(controller, media, ObserveListeningSettingsUseCase(settings), { text }, scope).start()
        runCurrent()
        controller.start(ids.map(::word), setOf("de", "en"), ListeningSelection())
        runCurrent()
        return controller
    }

    private val ListeningMediaState?.playing get() = assertIs<ListeningMediaState.Playing>(this)

    @Test
    fun `nothing is shown or released before a session starts`() = runTest(dispatcher) {
        ListeningMediaBridge(
            controller = ListeningPlaybackController(
                scope = scope,
                speakWord = SpeakWordUseCase(tts),
                stopSpeaking = StopSpeakingUseCase(tts),
                observeSettings = ObserveListeningSettingsUseCase(settings),
                observeSpeechRate = ObserveSpeechRateUseCase(FakeSettingsRepository()),
                analyticsTracker = FakeAnalyticsTracker(),
                recordSession = RecordListeningSessionUseCase(FakeListeningRecorder()),
                generateSessionId = GenerateSessionIdUseCase(Random(1)),
            ),
            session = media,
            observeSettings = ObserveListeningSettingsUseCase(settings),
            loadText = { text },
            scope = scope,
        ).start()
        runCurrent()

        assertTrue(media.rendered.isEmpty())
        assertEquals(0, media.releaseCount)
    }

    @Test
    fun `playing shows the word with its position and hides the answer until spoken`() = runTest(dispatcher) {
        started(1, 2)

        val prompt = media.last.playing
        assertEquals("Hund1", prompt.title)
        assertNull(prompt.subtitle)
        assertEquals("1 of 2 · German → English", prompt.detail)
        assertEquals(0, prompt.index)
        assertEquals(2, prompt.total)
        assertTrue(prompt.isPlaying)

        advanceTimeBy(pauseMs)
        runCurrent()
        assertEquals("dog1", media.last.playing.subtitle)
    }

    @Test
    fun `silent pause and gap steps don't re-render`() = runTest(dispatcher) {
        started(1, 2)
        val beforeAnswer = media.rendered.size

        advanceTimeBy(pauseMs - 1)
        runCurrent()

        assertEquals(beforeAnswer, media.rendered.size)
    }

    @Test
    fun `translation-first order swaps title, answer and languages`() = runTest(dispatcher) {
        settings.settings.value = ListeningSettings(order = ListeningOrder.TRANSLATION_FIRST)
        started(1)

        val prompt = media.last.playing
        assertEquals("dog1", prompt.title)
        assertEquals("1 of 1 · English → German", prompt.detail)
    }

    @Test
    fun `finished session offers repeat and close`() = runTest(dispatcher) {
        started(1, 2)
        advanceUntilIdle()

        val finished = assertIs<ListeningMediaState.Finished>(media.last)
        assertEquals("Nice listening!", finished.title)
        assertEquals("2 words · 1 min", finished.subtitle)
    }

    @Test
    fun `system buttons drive playback`() = runTest(dispatcher) {
        val controller = started(1, 2, 3)

        media.press(MediaCommand.Pause)
        runCurrent()
        assertFalse(media.last.playing.isPlaying)

        media.press(MediaCommand.Next)
        runCurrent()
        assertEquals(1, media.last.playing.index)

        media.press(MediaCommand.Play)
        runCurrent()
        assertTrue(media.last.playing.isPlaying)
        assertEquals(ListeningStep.Pause, (controller.playback.value as ListeningPlayback.Active).session.step)
    }

    @Test
    fun `repeat from the finished notification plays the words again`() = runTest(dispatcher) {
        started(1)
        advanceUntilIdle()

        media.press(MediaCommand.Repeat)
        runCurrent()

        val playing = media.last.playing
        assertEquals("Hund1", playing.title)
        assertTrue(playing.isPlaying)
    }

    @Test
    fun `close and stop release the notification`() = runTest(dispatcher) {
        val controller = started(1)
        advanceUntilIdle()

        media.press(MediaCommand.Close)
        runCurrent()
        assertEquals(ListeningPlayback.Idle, controller.playback.value)
        assertEquals(1, media.releaseCount)

        controller.repeat()
        runCurrent()
        media.press(MediaCommand.Stop)
        runCurrent()
        assertEquals(2, media.releaseCount)
    }

    @Test
    fun `mapper shows nothing for idle`() {
        assertNull(ListeningPlayback.Idle.toMediaState(ListeningOrder.WORD_FIRST, text))
        val active = ListeningPlayback.Active(
            session = ListeningSession(listOf(word(1)), step = ListeningStep.Gap),
            isPlaying = false,
            speakableLanguages = emptySet(),
        )
        val state = active.toMediaState(ListeningOrder.WORD_FIRST, text).playing
        assertEquals("dog1", state.subtitle)
        assertFalse(state.isPlaying)
    }
}
