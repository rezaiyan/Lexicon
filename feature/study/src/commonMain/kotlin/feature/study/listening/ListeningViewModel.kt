package feature.study.listening

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.getOrNull
import core.common.onFailure
import core.common.onSuccess
import domain.listening.model.ListeningCommand
import domain.listening.model.ListeningOrder
import domain.listening.model.ListeningQueue
import domain.listening.model.ListeningReducer
import domain.listening.model.ListeningSession
import domain.listening.model.ListeningSettings
import domain.listening.model.ListeningStep
import domain.listening.model.ListeningTransition
import domain.listening.model.ListeningVoiceCheck
import domain.listening.model.Utterance
import domain.listening.model.VoiceStatus
import domain.listening.usecase.BuildListeningQueueUseCase
import domain.listening.usecase.CheckListeningVoicesUseCase
import domain.listening.usecase.ObserveListeningSettingsUseCase
import domain.listening.usecase.SaveListeningSettingsUseCase
import domain.settings.usecase.ObserveSpeechRateUseCase
import domain.settings.usecase.SetTtsSpeechRateUseCase
import domain.tts.usecase.DownloadTtsModelUseCase
import domain.tts.usecase.SpeakWordUseCase
import domain.tts.usecase.StopSpeakingUseCase
import domain.word.model.ReviewSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import utils.Language
import kotlin.time.Clock

sealed interface ListeningScreenState {
    data object Idle : ListeningScreenState
    data object Loading : ListeningScreenState
    data object Empty : ListeningScreenState

    /** Pre-flight: some voices must be downloaded before playback can start cleanly. */
    data class NeedsVoices(
        val queue: ListeningQueue,
        val check: ListeningVoiceCheck,
        val download: VoiceDownload? = null,
        val downloadFailed: Boolean = false,
    ) : ListeningScreenState

    data class Active(
        val session: ListeningSession,
        val isPlaying: Boolean,
        val isRecentFallback: Boolean,
        /** Normalized language codes that have a ready voice; others are shown but not spoken. */
        val speakableLanguages: Set<String>,
        val heardWordIds: Set<Int> = emptySet(),
    ) : ListeningScreenState

    data class Finished(val wordsHeard: Int, val durationMs: Long) : ListeningScreenState

    /** No language in the queue has a voice on this device. */
    data object NoVoices : ListeningScreenState
    data class Error(val message: String) : ListeningScreenState
}

data class VoiceDownload(val languageCode: String, val progress: Float)

data class ListeningState(
    val screen: ListeningScreenState = ListeningScreenState.Idle,
    val settings: ListeningSettings = ListeningSettings(),
    val speechRate: Float = 1.0f,
)

/**
 * Hands-free audio review: speaks prompt → pause → answer → gap for each word.
 * Passive exposure only — never grades words, never touches SRS scheduling or streaks.
 */
@Suppress(
    "TooManyFunctions",   // each public method is a distinct event-sink entry point required by the screen
    "LongParameterList",  // all parameters are mandatory use-case dependencies injected by Koin
)
class ListeningViewModel(
    private val buildQueue: BuildListeningQueueUseCase,
    private val checkVoices: CheckListeningVoicesUseCase,
    private val downloadVoice: DownloadTtsModelUseCase,
    private val speakWord: SpeakWordUseCase,
    private val stopSpeaking: StopSpeakingUseCase,
    observeSettings: ObserveListeningSettingsUseCase,
    private val saveSettings: SaveListeningSettingsUseCase,
    observeSpeechRate: ObserveSpeechRateUseCase,
    private val setTtsSpeechRate: SetTtsSpeechRateUseCase,
    private val analyticsTracker: IAnalyticsTracker,
) : BaseViewModel<ListeningState, Nothing>() {

    override fun initialState() = ListeningState()

    private var playJob: Job? = null
    private var stopJob: Job? = null
    private var setupJob: Job? = null
    private var startedAt: Long = 0L

    init {
        observeSettings(Unit)
            .onEach { settings -> updateState { copy(settings = settings) } }
            .catch { }
            .launchIn(viewModelScope)

        observeSpeechRate(Unit)
            .onEach { rate -> updateState { copy(speechRate = rate) } }
            .catch { }
            .launchIn(viewModelScope)
    }

    // ---------------------------------------------------------------------------
    // Public event-sink API
    // ---------------------------------------------------------------------------

    fun start(source: ReviewSource) {
        haltPlayback()
        setupJob?.cancel()
        setupJob = viewModelScope.launch {
            updateState { copy(screen = ListeningScreenState.Loading) }
            buildQueue(source)
                .onSuccess { queue ->
                    if (queue.words.isEmpty()) {
                        updateState { copy(screen = ListeningScreenState.Empty) }
                    } else {
                        prepare(queue)
                    }
                }
                .onFailure { error ->
                    updateState { copy(screen = ListeningScreenState.Error(error.message.orEmpty())) }
                }
        }
    }

    /** Downloads every missing voice, then starts playback once all are ready. */
    fun downloadMissingVoices() {
        val needs = currentState.screen as? ListeningScreenState.NeedsVoices ?: return
        if (needs.download != null) return
        setupJob = viewModelScope.launch {
            var failed = false
            for (voice in needs.check.missing) {
                updateNeedsVoices { copy(download = VoiceDownload(voice.languageCode, 0f), downloadFailed = false) }
                downloadVoice(voice.languageCode)
                    .onEach { progress ->
                        updateNeedsVoices { copy(download = VoiceDownload(voice.languageCode, progress)) }
                    }
                    .catch { failed = true }
                    .collect { }
                if (failed) break
            }
            if (failed) {
                updateNeedsVoices { copy(download = null, downloadFailed = true) }
            } else {
                prepare(needs.queue)
            }
        }
    }

    /** Starts without the missing voices; those lines are shown but not spoken. */
    fun startWithoutMissingVoices() {
        val needs = currentState.screen as? ListeningScreenState.NeedsVoices ?: return
        setupJob?.cancel()
        begin(needs.queue, needs.check)
    }

    fun togglePlayback() {
        val active = currentState.screen as? ListeningScreenState.Active ?: return
        if (active.isPlaying) {
            haltPlayback()
            updateActive { copy(isPlaying = false) }
        } else {
            updateActive { copy(isPlaying = true) }
            play()
        }
    }

    fun next() = jump(ListeningCommand.Next)

    fun previous() = jump(ListeningCommand.Previous)

    fun setPause(pauseMs: Long) = persist { copy(pauseMs = pauseMs) }

    fun setOrder(order: ListeningOrder) = persist { copy(order = order) }

    fun setRepeatCount(count: Int) = persist { copy(repeatCount = count) }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch { setTtsSpeechRate(rate) }
    }

    /** User left the screen mid-session. */
    fun abandon() {
        (currentState.screen as? ListeningScreenState.Active)?.let { logSession(it, completed = false) }
        haltPlayback()
        setupJob?.cancel()
        updateState { copy(screen = ListeningScreenState.Idle) }
    }

    override fun onCleared() {
        super.onCleared()
        playJob?.cancel()
        setupJob?.cancel()
        (currentState.screen as? ListeningScreenState.Active)?.let { logSession(it, completed = false) }
        viewModelScope.launch(NonCancellable) { stopSpeaking() }
    }

    // ---------------------------------------------------------------------------
    // Setup
    // ---------------------------------------------------------------------------

    private suspend fun prepare(queue: ListeningQueue) {
        val check = checkVoices(queue.words).getOrNull()
            ?: ListeningVoiceCheck(emptyList())
        when {
            !check.canSpeakAnything -> updateState { copy(screen = ListeningScreenState.NoVoices) }
            check.missing.isNotEmpty() ->
                updateState { copy(screen = ListeningScreenState.NeedsVoices(queue, check)) }
            else -> begin(queue, check)
        }
    }

    private fun begin(queue: ListeningQueue, check: ListeningVoiceCheck) {
        startedAt = Clock.System.now().toEpochMilliseconds()
        val speakable = check.voices
            .filter { it.status == VoiceStatus.READY }
            .mapTo(mutableSetOf()) { it.languageCode }
        updateState {
            copy(
                screen = ListeningScreenState.Active(
                    session = ListeningSession(queue.words),
                    isPlaying = true,
                    isRecentFallback = queue.isRecentFallback,
                    speakableLanguages = speakable,
                )
            )
        }
        analyticsTracker.logEvent("listening_session_start", mapOf("word_count" to queue.words.size))
        play()
    }

    // ---------------------------------------------------------------------------
    // Playback
    // ---------------------------------------------------------------------------

    private fun play() {
        playJob?.cancel()
        val pendingStop = stopJob
        playJob = viewModelScope.launch {
            pendingStop?.join()
            while (true) {
                val active = currentState.screen as? ListeningScreenState.Active ?: return@launch
                performStep(active)
                val transition = ListeningReducer.reduce(
                    active.session,
                    ListeningCommand.StepCompleted,
                    currentState.settings.repeatCount,
                )
                val heard = if (active.session.step == ListeningStep.Answer) {
                    active.heardWordIds + active.session.currentWord.id
                } else {
                    active.heardWordIds
                }
                when (transition) {
                    is ListeningTransition.Continue ->
                        updateActive { copy(session = transition.session, heardWordIds = heard) }
                    ListeningTransition.Finished -> {
                        finish(active.copy(heardWordIds = heard))
                        return@launch
                    }
                }
            }
        }
    }

    private suspend fun performStep(active: ListeningScreenState.Active) {
        val settings = currentState.settings
        val session = active.session
        when (session.step) {
            ListeningStep.Prompt -> speak(session.prompt(settings.order), active.speakableLanguages)
            ListeningStep.Pause -> delay(settings.pauseMs)
            ListeningStep.Answer -> speak(session.answer(settings.order), active.speakableLanguages)
            ListeningStep.Gap -> delay(settings.gapMs)
        }
    }

    private suspend fun speak(utterance: Utterance, speakable: Set<String>) {
        if (Language.toCode(utterance.languageCode) !in speakable) return
        speakWord(utterance.text, utterance.languageCode)
    }

    private fun jump(command: ListeningCommand) {
        val active = currentState.screen as? ListeningScreenState.Active ?: return
        haltPlayback()
        when (val transition = ListeningReducer.reduce(active.session, command, currentState.settings.repeatCount)) {
            is ListeningTransition.Continue -> {
                updateActive { copy(session = transition.session) }
                if (active.isPlaying) play()
            }
            ListeningTransition.Finished -> finish(active)
        }
    }

    /** Cancels the sequencer and silences any in-flight synthesis or playback. */
    private fun haltPlayback() {
        val job = playJob ?: return
        playJob = null
        job.cancel()
        stopJob = viewModelScope.launch { stopSpeaking() }
    }

    private fun finish(active: ListeningScreenState.Active) {
        val durationMs = Clock.System.now().toEpochMilliseconds() - startedAt
        logSession(active, completed = true)
        updateState {
            copy(screen = ListeningScreenState.Finished(active.heardWordIds.size, durationMs))
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun persist(change: ListeningSettings.() -> ListeningSettings) {
        val updated = currentState.settings.change()
        updateState { copy(settings = updated) }
        viewModelScope.launch { saveSettings(updated) }
    }

    private fun logSession(active: ListeningScreenState.Active, completed: Boolean) {
        analyticsTracker.logEvent(
            if (completed) "listening_session_complete" else "listening_session_abandoned",
            mapOf(
                "words_heard" to active.heardWordIds.size,
                "word_count" to active.session.words.size,
                "duration_ms" to Clock.System.now().toEpochMilliseconds() - startedAt,
            ),
        )
    }

    private fun updateActive(reducer: ListeningScreenState.Active.() -> ListeningScreenState.Active) {
        val active = currentState.screen as? ListeningScreenState.Active ?: return
        updateState { copy(screen = active.reducer()) }
    }

    private fun updateNeedsVoices(
        reducer: ListeningScreenState.NeedsVoices.() -> ListeningScreenState.NeedsVoices,
    ) {
        val needs = currentState.screen as? ListeningScreenState.NeedsVoices ?: return
        updateState { copy(screen = needs.reducer()) }
    }
}
