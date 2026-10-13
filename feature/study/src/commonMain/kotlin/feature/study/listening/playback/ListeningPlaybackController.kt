package feature.study.listening.playback

import analytics.IAnalyticsTracker
import domain.listening.model.ListeningCommand
import domain.listening.model.ListeningReducer
import domain.listening.model.ListeningSelection
import domain.listening.model.ListeningSession
import domain.listening.model.ListeningSessionTracker
import domain.listening.model.ListeningSettings
import domain.listening.model.ListeningSource
import domain.listening.model.ListeningStep
import domain.listening.model.ListeningTransition
import domain.listening.model.Utterance
import domain.listening.usecase.ObserveListeningSettingsUseCase
import domain.listening.usecase.RecordListeningSessionUseCase
import domain.settings.usecase.ObserveSpeechRateUseCase
import domain.study.usecase.GenerateSessionIdUseCase
import domain.tts.usecase.SpeakWordUseCase
import domain.tts.usecase.StopSpeakingUseCase
import domain.word.model.Word
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import utils.Language
import kotlin.time.Clock

/** What listening playback is doing right now; every surface (screen, mini player, notification) renders this. */
sealed interface ListeningPlayback {
    data object Idle : ListeningPlayback

    data class Active(
        val session: ListeningSession,
        val isPlaying: Boolean,
        /** Normalized language codes that have a ready voice; others are shown but not spoken. */
        val speakableLanguages: Set<String>,
        val heardWordIds: Set<Int> = emptySet(),
    ) : ListeningPlayback

    data class Finished(val wordsHeard: Int, val totalWords: Int, val durationMs: Long) : ListeningPlayback
}

/**
 * Plays listening sessions: speaks prompt → pause → answer → example → gap for each word.
 *
 * App-scoped (one instance, its own [scope]) so playback outlives the screen: it keeps going when
 * the app is backgrounded, the overlay is collapsed, or the hosting ViewModel is cleared.
 * Passive exposure only — never grades words, never touches SRS scheduling or streaks.
 */
@Suppress("TooManyFunctions", "LongParameterList") // one public method per playback command; Koin-injected use cases
class ListeningPlaybackController(
    private val scope: CoroutineScope,
    private val speakWord: SpeakWordUseCase,
    private val stopSpeaking: StopSpeakingUseCase,
    observeSettings: ObserveListeningSettingsUseCase,
    observeSpeechRate: ObserveSpeechRateUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val recordSession: RecordListeningSessionUseCase,
    private val generateSessionId: GenerateSessionIdUseCase,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val _playback = MutableStateFlow<ListeningPlayback>(ListeningPlayback.Idle)
    val playback: StateFlow<ListeningPlayback> = _playback.asStateFlow()

    private var settings = ListeningSettings()
    private var speechRate = 1.0f

    private var playJob: Job? = null
    private var stopJob: Job? = null
    private var startedAt = 0L

    /** When the current word's prompt last began; drives the "previous" restart-vs-go-back rule. */
    private var wordStartedAt = 0L

    /** The words and voices of the last session, so it can be repeated without rebuilding the queue. */
    private var lastRun: Run? = null

    /** What the current session has done so far; null when no session is running. */
    private var tracker: ListeningSessionTracker? = null

    private val active: ListeningPlayback.Active? get() = _playback.value as? ListeningPlayback.Active

    init {
        observeSettings(Unit)
            .onEach { settings = it }
            .catch { }
            .launchIn(scope)
        observeSpeechRate(Unit)
            .onEach { speechRate = it }
            .catch { }
            .launchIn(scope)
    }

    // ---------------------------------------------------------------------------
    // Commands
    // ---------------------------------------------------------------------------

    /** Starts playing [words]; ends any session that is still running. */
    fun start(words: List<Word>, speakableLanguages: Set<String>, selection: ListeningSelection) {
        if (words.isEmpty()) return
        val run = Run(words, speakableLanguages, selection)
        lastRun = run
        begin(run)
    }

    /** Plays the last session's words again from the start, as a fresh session. */
    fun repeat() {
        val run = lastRun ?: return
        begin(run)
    }

    fun togglePlayback() {
        val current = active ?: return
        if (current.isPlaying) pause() else resume()
    }

    fun pause() {
        val current = active ?: return
        if (!current.isPlaying) return
        haltPlayback()
        tracker = tracker?.paused(now())
        updateActive { copy(isPlaying = false) }
    }

    /** Resumes from the current word's prompt, so the pair is always heard together. */
    fun resume() {
        val current = active ?: return
        if (current.isPlaying) return
        tracker = tracker?.resumed(now())
        updateActive { copy(session = session.copy(step = ListeningStep.Prompt), isPlaying = true) }
        play()
    }

    fun next() {
        if (active != null) tracker = tracker?.skipped()
        jump(ListeningCommand.Next)
    }

    /** Music-player rule: early in a word goes back a word, later restarts the current one. */
    fun previous() {
        if (active == null) return
        val restart = now() - wordStartedAt > PREVIOUS_RESTART_THRESHOLD_MS
        jump(if (restart) ListeningCommand.Restart else ListeningCommand.Previous)
    }

    /** Stops and forgets the session: an unfinished one is recorded as abandoned. */
    fun close() {
        active?.let { endSession(it, completed = false) }
        haltPlayback()
        _playback.value = ListeningPlayback.Idle
    }

    // ---------------------------------------------------------------------------
    // Playback
    // ---------------------------------------------------------------------------

    private fun begin(run: Run) {
        active?.let { endSession(it, completed = false) }
        haltPlayback()
        startedAt = now()
        wordStartedAt = startedAt
        tracker = ListeningSessionTracker(
            clientSessionId = generateSessionId(),
            selection = run.selection,
            settings = settings,
            speechRate = speechRate,
            plannedWords = run.words.size,
            startedAt = startedAt,
        )
        _playback.value = ListeningPlayback.Active(
            session = ListeningSession(run.words),
            isPlaying = true,
            speakableLanguages = run.speakableLanguages,
        )
        analyticsTracker.logEvent(
            "listening_session_start",
            mapOf("word_count" to run.words.size, "source" to run.selection.source.analyticsName),
        )
        play()
    }

    private fun play() {
        playJob?.cancel()
        val pendingStop = stopJob
        playJob = scope.launch {
            pendingStop?.join()
            while (true) {
                val current = active ?: return@launch
                performStep(current)
                val transition = ListeningReducer.reduce(
                    current.session,
                    ListeningCommand.StepCompleted,
                    settings.repeatCount,
                )
                val heard = if (current.session.step == ListeningStep.Answer) {
                    tracker = tracker?.heard(current.session.currentWord, now())
                    current.heardWordIds + current.session.currentWord.id
                } else {
                    current.heardWordIds
                }
                when (transition) {
                    is ListeningTransition.Continue ->
                        updateActive { copy(session = transition.session, heardWordIds = heard) }
                    ListeningTransition.Finished -> {
                        finish(current.copy(heardWordIds = heard))
                        return@launch
                    }
                }
            }
        }
    }

    private suspend fun performStep(current: ListeningPlayback.Active) {
        val session = current.session
        when (session.step) {
            ListeningStep.Prompt -> {
                wordStartedAt = now()
                speak(session.prompt(settings.order), current.speakableLanguages)
            }
            ListeningStep.Pause -> delay(settings.pauseMs)
            ListeningStep.Answer -> speak(session.answer(settings.order), current.speakableLanguages)
            ListeningStep.Example -> session.example()?.let { speak(it, current.speakableLanguages) }
            ListeningStep.Gap -> delay(settings.gapMs)
        }
    }

    private suspend fun speak(utterance: Utterance, speakable: Set<String>) {
        if (Language.toCode(utterance.languageCode) !in speakable) return
        speakWord(utterance.text, utterance.languageCode)
    }

    private fun jump(command: ListeningCommand) {
        val current = active ?: return
        haltPlayback()
        wordStartedAt = now()
        when (val transition = ListeningReducer.reduce(current.session, command, settings.repeatCount)) {
            is ListeningTransition.Continue -> {
                updateActive { copy(session = transition.session) }
                if (current.isPlaying) play()
            }
            ListeningTransition.Finished -> finish(current)
        }
    }

    /** Cancels the sequencer and silences any in-flight synthesis or playback. */
    private fun haltPlayback() {
        val job = playJob ?: return
        playJob = null
        job.cancel()
        stopJob = scope.launch { stopSpeaking() }
    }

    private fun finish(current: ListeningPlayback.Active) {
        endSession(current, completed = true)
        _playback.value = ListeningPlayback.Finished(
            wordsHeard = current.heardWordIds.size,
            totalWords = current.session.words.size,
            durationMs = now() - startedAt,
        )
    }

    /** Logs the session and stores it for study insights; runs once per session. */
    private fun endSession(current: ListeningPlayback.Active, completed: Boolean) {
        analyticsTracker.logEvent(
            if (completed) "listening_session_complete" else "listening_session_abandoned",
            mapOf(
                "words_heard" to current.heardWordIds.size,
                "word_count" to current.session.words.size,
                "duration_ms" to now() - startedAt,
            ),
        )
        val record = tracker?.end(now(), completed) ?: return
        tracker = null
        scope.launch(NonCancellable) { recordSession(record) }
    }

    private fun updateActive(reducer: ListeningPlayback.Active.() -> ListeningPlayback.Active) {
        _playback.update { current -> (current as? ListeningPlayback.Active)?.reducer() ?: current }
    }

    private val ListeningSource.analyticsName: String
        get() = when (this) {
            ListeningSource.Due -> "due"
            ListeningSource.All -> "all"
            is ListeningSource.Level -> "level"
            is ListeningSource.Tag -> "tag"
        }

    private data class Run(
        val words: List<Word>,
        val speakableLanguages: Set<String>,
        val selection: ListeningSelection,
    )

    companion object {
        /** Past this far into a word, "previous" restarts it instead of going back a word. */
        const val PREVIOUS_RESTART_THRESHOLD_MS = 2_000L
    }
}
