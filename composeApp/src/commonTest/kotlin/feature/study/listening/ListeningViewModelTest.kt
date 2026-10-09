package feature.study.listening

import domain.focus.model.LearningFocus
import domain.focus.usecase.ObserveLearningFocusUseCase
import domain.listening.model.ListeningOrder
import domain.listening.model.ListeningSettings
import domain.listening.usecase.BuildListeningQueueUseCase
import domain.listening.usecase.CheckListeningVoicesUseCase
import domain.listening.usecase.ObserveHasListeningWordsUseCase
import domain.listening.usecase.ObserveListeningSettingsUseCase
import domain.listening.usecase.SaveListeningSettingsUseCase
import domain.settings.usecase.GetDailyGoalWordsUseCase
import domain.settings.usecase.ObserveSpeechRateUseCase
import domain.settings.usecase.SetTtsSpeechRateUseCase
import domain.tts.usecase.DownloadTtsModelUseCase
import domain.tts.usecase.SpeakWordUseCase
import domain.tts.usecase.StopSpeakingUseCase
import domain.word.model.ReviewSource
import domain.word.model.Word
import domain.word.usecase.GetDueWordsByTagUseCase
import domain.word.usecase.GetDueWordsUseCase
import domain.word.usecase.GetWordsByStageUseCase
import domain.word.usecase.LoadReviewQueueUseCase
import fakes.FakeAnalyticsTracker
import fakes.FakeLearningFocusRepository
import fakes.FakeListeningSettingsRepository
import fakes.FakeSettingsRepository
import fakes.FakeTtsRepository
import fakes.FakeWordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import utils.Language
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ListeningViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val wordRepository = FakeWordRepository()
    private val ttsRepository = FakeTtsRepository()
    private val listeningSettings = FakeListeningSettingsRepository()
    private val analytics = FakeAnalyticsTracker()

    private val pauseMs = ListeningSettings.DEFAULT_PAUSE_MS

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
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

    private fun givenDueWords(vararg ids: Int) {
        val words = ids.map(::word)
        wordRepository.dueWords = words
        wordRepository.storedWords = words.toMutableList()
    }

    private fun createViewModel(): ListeningViewModel {
        val settings = FakeSettingsRepository()
        val observeFocus = ObserveLearningFocusUseCase(wordRepository, FakeLearningFocusRepository(LearningFocus.All))
        val loadQueue = LoadReviewQueueUseCase(
            GetDueWordsUseCase(wordRepository),
            GetWordsByStageUseCase(wordRepository),
            GetDueWordsByTagUseCase(wordRepository),
            GetDailyGoalWordsUseCase(settings),
            observeFocus,
        )
        return ListeningViewModel(
            buildQueue = BuildListeningQueueUseCase(loadQueue, wordRepository, observeFocus),
            checkVoices = CheckListeningVoicesUseCase(ttsRepository),
            downloadVoice = DownloadTtsModelUseCase(ttsRepository),
            speakWord = SpeakWordUseCase(ttsRepository),
            stopSpeaking = StopSpeakingUseCase(ttsRepository),
            observeSettings = ObserveListeningSettingsUseCase(listeningSettings),
            observeHasWords = ObserveHasListeningWordsUseCase(wordRepository, observeFocus),
            saveSettings = SaveListeningSettingsUseCase(listeningSettings),
            observeSpeechRate = ObserveSpeechRateUseCase(settings),
            setTtsSpeechRate = SetTtsSpeechRateUseCase(settings),
            analyticsTracker = analytics,
        )
    }

    private fun TestScope.startedViewModel(): ListeningViewModel =
        createViewModel().also {
            it.start(ReviewSource.DueCards)
            runCurrent()
        }

    private val ListeningViewModel.active get() = currentState.screen as ListeningScreenState.Active

    @Test
    fun `start speaks word then translation after the pause`() = runTest(dispatcher) {
        givenDueWords(1, 2)
        val viewModel = startedViewModel()

        assertTrue(viewModel.active.isPlaying)
        assertEquals(listOf("Hund1" to "de"), ttsRepository.spoken)

        advanceTimeBy(pauseMs - 1)
        runCurrent()
        assertEquals(1, ttsRepository.spoken.size)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("Hund1" to "de", "dog1" to "en"), ttsRepository.spoken)
        assertTrue(viewModel.active.session.isAnswerRevealed)
    }

    @Test
    fun `full session finishes with every word heard and logs completion`() = runTest(dispatcher) {
        givenDueWords(1, 2)
        val viewModel = startedViewModel()

        advanceUntilIdle()

        val finished = assertIs<ListeningScreenState.Finished>(viewModel.currentState.screen)
        assertEquals(2, finished.wordsHeard)
        assertEquals(
            listOf("Hund1" to "de", "dog1" to "en", "Hund2" to "de", "dog2" to "en"),
            ttsRepository.spoken,
        )
        assertTrue(analytics.events.any { it.first == "listening_session_complete" })
    }

    @Test
    fun `listening never grades words or changes scheduling`() = runTest(dispatcher) {
        givenDueWords(1, 2)
        startedViewModel()

        advanceUntilIdle()

        assertEquals(0, wordRepository.updateCallCount)
        assertEquals(0, wordRepository.updateLocalCallCount)
        assertEquals(0, wordRepository.batchSyncCallCount)
        assertTrue(wordRepository.updatedWords.isEmpty())
    }

    @Test
    fun `translation-first order speaks the translation as the prompt`() = runTest(dispatcher) {
        givenDueWords(1)
        listeningSettings.settings.value = ListeningSettings(order = ListeningOrder.TRANSLATION_FIRST)
        startedViewModel()

        advanceUntilIdle()

        assertEquals(listOf("dog1" to "en", "Hund1" to "de"), ttsRepository.spoken)
    }

    @Test
    fun `repeat count plays each word that many times`() = runTest(dispatcher) {
        givenDueWords(1)
        listeningSettings.settings.value = ListeningSettings(repeatCount = 2)
        startedViewModel()

        advanceUntilIdle()

        assertEquals(4, ttsRepository.spoken.size)
    }

    @Test
    fun `pausing stops speech and holds position until resumed`() = runTest(dispatcher) {
        givenDueWords(1, 2)
        val viewModel = startedViewModel()

        viewModel.togglePlayback()
        runCurrent()
        advanceTimeBy(pauseMs * 10)
        runCurrent()

        assertFalse(viewModel.active.isPlaying)
        assertEquals(1, ttsRepository.spoken.size)
        assertTrue(ttsRepository.stopCount > 0)

        viewModel.togglePlayback()
        advanceTimeBy(pauseMs)
        runCurrent()
        assertEquals("dog1" to "en", ttsRepository.spoken.last())
    }

    @Test
    fun `next skips to the next word prompt`() = runTest(dispatcher) {
        givenDueWords(1, 2)
        val viewModel = startedViewModel()

        viewModel.next()
        runCurrent()

        assertEquals(1, viewModel.active.session.index)
        assertEquals("Hund2" to "de", ttsRepository.spoken.last())
    }

    @Test
    fun `next while paused moves without speaking`() = runTest(dispatcher) {
        givenDueWords(1, 2)
        val viewModel = startedViewModel()
        viewModel.togglePlayback()
        runCurrent()

        viewModel.next()
        advanceUntilIdle()

        assertEquals(1, viewModel.active.session.index)
        assertEquals(1, ttsRepository.spoken.size)
    }

    @Test
    fun `previous replays the earlier word`() = runTest(dispatcher) {
        givenDueWords(1, 2)
        val viewModel = startedViewModel()
        viewModel.next()
        runCurrent()

        viewModel.previous()
        runCurrent()

        assertEquals(0, viewModel.active.session.index)
        assertEquals("Hund1" to "de", ttsRepository.spoken.last())
    }

    @Test
    fun `missing voice requires download before playback`() = runTest(dispatcher) {
        givenDueWords(1)
        ttsRepository.missingLanguages += "de"
        val viewModel = startedViewModel()

        val needs = assertIs<ListeningScreenState.NeedsVoices>(viewModel.currentState.screen)
        assertEquals(listOf("de"), needs.check.missing.map { it.languageCode })
        assertTrue(ttsRepository.spoken.isEmpty())

        viewModel.downloadMissingVoices()
        advanceUntilIdle()

        assertIs<ListeningScreenState.Finished>(viewModel.currentState.screen)
        assertTrue("Hund1" to "de" in ttsRepository.spoken)
    }

    @Test
    fun `failed voice download stays in pre-flight with an error`() = runTest(dispatcher) {
        givenDueWords(1)
        ttsRepository.missingLanguages += "de"
        ttsRepository.downloadShouldFail = true
        val viewModel = startedViewModel()

        viewModel.downloadMissingVoices()
        advanceUntilIdle()

        val needs = assertIs<ListeningScreenState.NeedsVoices>(viewModel.currentState.screen)
        assertTrue(needs.downloadFailed)
        assertEquals(null, needs.download)
    }

    @Test
    fun `retrying a failed download skips voices that already finished`() = runTest(dispatcher) {
        givenDueWords(1)
        ttsRepository.missingLanguages += setOf("de", "en")
        ttsRepository.failingDownloads += "en"
        val viewModel = startedViewModel()

        viewModel.downloadMissingVoices()
        advanceUntilIdle()

        val needs = assertIs<ListeningScreenState.NeedsVoices>(viewModel.currentState.screen)
        assertTrue(needs.downloadFailed)
        assertEquals(listOf("en"), needs.check.missing.map { it.languageCode })

        ttsRepository.failingDownloads.clear()
        ttsRepository.downloadRequests.clear()
        viewModel.downloadMissingVoices()
        advanceUntilIdle()

        assertEquals(listOf("en"), ttsRepository.downloadRequests)
        assertIs<ListeningScreenState.Finished>(viewModel.currentState.screen)
    }

    @Test
    fun `voice download progress covers every missing voice in one pass`() {
        val first = VoiceDownload(languageCode = "de", position = 1, total = 2, progress = 0.5f)
        val second = VoiceDownload(languageCode = "en", position = 2, total = 2, progress = 0.5f)

        assertEquals(0.25f, first.overallProgress)
        assertEquals(0.75f, second.overallProgress)
        assertEquals(1f, second.copy(progress = 1f).overallProgress)
    }

    @Test
    fun `starting without missing voices skips only those lines`() = runTest(dispatcher) {
        givenDueWords(1)
        ttsRepository.missingLanguages += "de"
        val viewModel = startedViewModel()

        viewModel.startWithoutMissingVoices()
        advanceUntilIdle()

        assertEquals(listOf("dog1" to "en"), ttsRepository.spoken)
        assertIs<ListeningScreenState.Finished>(viewModel.currentState.screen)
    }

    @Test
    fun `no supported voices shows NoVoices`() = runTest(dispatcher) {
        givenDueWords(1)
        ttsRepository.languageSupported = false

        val viewModel = startedViewModel()

        assertIs<ListeningScreenState.NoVoices>(viewModel.currentState.screen)
    }

    @Test
    fun `hasWords reflects whether there is anything to listen to`() = runTest(dispatcher) {
        val empty = createViewModel()
        runCurrent()
        assertFalse(empty.currentState.hasWords)

        givenDueWords(1)
        val withWords = createViewModel()
        runCurrent()
        assertTrue(withWords.currentState.hasWords)
    }

    @Test
    fun `no words shows Empty`() = runTest(dispatcher) {
        val viewModel = startedViewModel()

        assertIs<ListeningScreenState.Empty>(viewModel.currentState.screen)
    }

    @Test
    fun `abandon stops playback and logs an abandoned session`() = runTest(dispatcher) {
        givenDueWords(1, 2)
        val viewModel = startedViewModel()

        viewModel.abandon()
        advanceUntilIdle()

        assertIs<ListeningScreenState.Idle>(viewModel.currentState.screen)
        assertEquals(1, ttsRepository.spoken.size)
        assertTrue(ttsRepository.stopCount > 0)
        assertTrue(analytics.events.any { it.first == "listening_session_abandoned" })
    }

    @Test
    fun `changing settings persists them`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.setPause(5_000L)
        viewModel.setOrder(ListeningOrder.TRANSLATION_FIRST)
        viewModel.setRepeatCount(2)
        runCurrent()

        assertEquals(
            ListeningSettings(pauseMs = 5_000L, order = ListeningOrder.TRANSLATION_FIRST, repeatCount = 2),
            listeningSettings.settings.value,
        )
    }
}
