package feature.study.listening

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.getOrNull
import core.common.onFailure
import core.common.onSuccess
import domain.listening.model.ListeningOptions
import domain.listening.model.ListeningOrder
import domain.listening.model.ListeningSelection
import domain.listening.model.ListeningSession
import domain.listening.model.ListeningSettings
import domain.listening.model.ListeningSource
import domain.listening.model.ListeningVoiceCheck
import domain.listening.model.VoiceStatus
import domain.listening.usecase.BuildListeningQueueUseCase
import domain.listening.usecase.CheckListeningVoicesUseCase
import domain.listening.usecase.ObserveListeningOptionsUseCase
import domain.listening.usecase.ObserveListeningSettingsUseCase
import domain.listening.usecase.SaveListeningSettingsUseCase
import domain.settings.usecase.ObserveSpeechRateUseCase
import domain.settings.usecase.SetTtsSpeechRateUseCase
import domain.tts.usecase.DownloadTtsModelUseCase
import domain.word.model.Word
import feature.study.listening.playback.ListeningPlayback
import feature.study.listening.playback.ListeningPlaybackController
import feature.study.listening.playback.ListeningPlayerVisibility
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed interface ListeningScreenState {
    data object Idle : ListeningScreenState

    /** Choosing which words to play, how many, and how. */
    data object Setup : ListeningScreenState
    data object Loading : ListeningScreenState
    data object Empty : ListeningScreenState

    /** Pre-flight: some voices must be downloaded before playback can start cleanly. */
    data class NeedsVoices(
        val words: List<Word>,
        val check: ListeningVoiceCheck,
        val download: VoiceDownload? = null,
        val downloadFailed: Boolean = false,
    ) : ListeningScreenState

    data class Active(
        val session: ListeningSession,
        val isPlaying: Boolean,
        /** Normalized language codes that have a ready voice; others are shown but not spoken. */
        val speakableLanguages: Set<String>,
        val heardWordIds: Set<Int> = emptySet(),
    ) : ListeningScreenState

    data class Finished(val wordsHeard: Int, val durationMs: Long) : ListeningScreenState

    /** No language in the queue has a voice on this device. */
    data object NoVoices : ListeningScreenState
    data class Error(val message: String) : ListeningScreenState
}

/** Download of voice [position] (1-based) of [total]; [progress] is for this voice alone. */
data class VoiceDownload(
    val languageCode: String,
    val position: Int,
    val total: Int,
    val progress: Float,
) {
    /** Progress across all voices in this pass, so the bar fills once instead of once per voice. */
    val overallProgress: Float get() = ((position - 1) + progress) / total
}

data class ListeningState(
    val screen: ListeningScreenState = ListeningScreenState.Idle,
    val settings: ListeningSettings = ListeningSettings(),
    val speechRate: Float = 1.0f,
    val options: ListeningOptions = ListeningOptions(),
) {
    /** Whether a session would have any words; drives the Study-tab card's CTA. */
    val hasWords: Boolean get() = options.hasWords

    /** The saved selection, moved to every word when its source has nothing to play right now. */
    val selection: ListeningSelection get() = options.resolve(settings.selection)

    /** Words the current selection would play. */
    val sessionSize: Int get() = selection.sessionSize(options.count(selection.source))

    /** A session is playing (not paused), so closing the screen should keep it going in the mini player. */
    val isPlaying: Boolean get() = (screen as? ListeningScreenState.Active)?.isPlaying == true
}

/**
 * Hands-free audio review screen: setup and voice pre-flight happen here; playback itself runs in
 * the app-scoped [ListeningPlaybackController] so it survives backgrounding and this ViewModel.
 */
@Suppress(
    "TooManyFunctions",   // each public method is a distinct event-sink entry point required by the screen
    "LongParameterList",  // all parameters are mandatory use-case dependencies injected by Koin
)
class ListeningViewModel(
    private val buildQueue: BuildListeningQueueUseCase,
    private val checkVoices: CheckListeningVoicesUseCase,
    private val downloadVoice: DownloadTtsModelUseCase,
    private val playback: ListeningPlaybackController,
    private val visibility: ListeningPlayerVisibility,
    observeSettings: ObserveListeningSettingsUseCase,
    observeOptions: ObserveListeningOptionsUseCase,
    private val saveSettings: SaveListeningSettingsUseCase,
    observeSpeechRate: ObserveSpeechRateUseCase,
    private val setTtsSpeechRate: SetTtsSpeechRateUseCase,
) : BaseViewModel<ListeningState, Nothing>() {

    override fun initialState() = ListeningState()

    private var setupJob: Job? = null

    init {
        observeSettings(Unit)
            .onEach { settings -> updateState { copy(settings = settings) } }
            .catch { }
            .launchIn(viewModelScope)

        observeSpeechRate(Unit)
            .onEach { rate -> updateState { copy(speechRate = rate) } }
            .catch { }
            .launchIn(viewModelScope)

        observeOptions(Unit)
            .onEach { options -> updateState { copy(options = options) } }
            .catch { }
            .launchIn(viewModelScope)

        playback.playback
            .onEach(::onPlayback)
            .launchIn(viewModelScope)
    }

    // ---------------------------------------------------------------------------
    // Public event-sink API
    // ---------------------------------------------------------------------------

    /** Shows the setup step, where the user picks words — unless a session is still playing, which stays. */
    fun open() {
        setupJob?.cancel()
        if (playback.playback.value is ListeningPlayback.Active) return
        playback.close()
        updateState { copy(screen = ListeningScreenState.Setup) }
    }

    /** Reopens on the live session (mini player, notification), or on setup when nothing is running. */
    fun reveal() {
        if (playback.playback.value == ListeningPlayback.Idle) open()
    }

    fun selectSource(source: ListeningSource) = persistSelection { copy(source = source) }

    fun setWordLimit(limit: Int) = persistSelection { copy(limit = limit) }

    fun setShuffle(shuffle: Boolean) = persistSelection { copy(shuffle = shuffle) }

    /** Plays the current selection; also "Listen again" and retry after an error. */
    fun start() {
        playback.close()
        setupJob?.cancel()
        val selection = currentState.selection
        setupJob = viewModelScope.launch {
            updateState { copy(screen = ListeningScreenState.Loading) }
            buildQueue(selection)
                .onSuccess { words ->
                    if (words.isEmpty()) {
                        updateState { copy(screen = ListeningScreenState.Empty) }
                    } else {
                        prepare(words)
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
        val missing = needs.check.missing
        setupJob = viewModelScope.launch {
            var failed = false
            for ((index, voice) in missing.withIndex()) {
                val download = VoiceDownload(voice.languageCode, index + 1, missing.size, 0f)
                updateNeedsVoices { copy(download = download, downloadFailed = false) }
                downloadVoice(voice.languageCode)
                    .onEach { progress ->
                        updateNeedsVoices { copy(download = download.copy(progress = progress)) }
                    }
                    .catch { failed = true }
                    .collect { }
                if (failed) break
                // Mark it ready so the row updates and a retry after a later failure skips it.
                updateNeedsVoices { copy(check = check.markReady(voice.languageCode)) }
            }
            if (failed) {
                updateNeedsVoices { copy(download = null, downloadFailed = true) }
            } else {
                prepare(needs.words)
            }
        }
    }

    /** Starts without the missing voices; those lines are shown but not spoken. */
    fun startWithoutMissingVoices() {
        val needs = currentState.screen as? ListeningScreenState.NeedsVoices ?: return
        setupJob?.cancel()
        begin(needs.words, needs.check)
    }

    fun togglePlayback() = playback.togglePlayback()

    fun next() = playback.next()

    fun previous() = playback.previous()

    fun setPause(pauseMs: Long) = persist { copy(pauseMs = pauseMs) }

    fun setOrder(order: ListeningOrder) = persist { copy(order = order) }

    fun setRepeatCount(count: Int) = persist { copy(repeatCount = count) }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch { setTtsSpeechRate(rate) }
    }

    /** The full player is on screen (or not); the mini player hides meanwhile. */
    fun setVisible(visible: Boolean) = visibility.setOpen(visible)

    val isPlayerVisible: Boolean get() = visibility.isOpen.value

    /** Something else is about to speak (e.g. a review): pause rather than talk over it. */
    fun pauseForOtherAudio() = playback.pause()

    /** User stopped listening (close while paused, on setup or on the summary): nothing keeps running. */
    fun abandon() {
        playback.close()
        setupJob?.cancel()
        updateState { copy(screen = ListeningScreenState.Idle) }
    }

    // ---------------------------------------------------------------------------
    // Setup
    // ---------------------------------------------------------------------------

    private suspend fun prepare(words: List<Word>) {
        val check = checkVoices(words).getOrNull()
            ?: ListeningVoiceCheck(emptyList())
        when {
            !check.canSpeakAnything -> updateState { copy(screen = ListeningScreenState.NoVoices) }
            check.missing.isNotEmpty() ->
                updateState { copy(screen = ListeningScreenState.NeedsVoices(words, check)) }
            else -> begin(words, check)
        }
    }

    private fun begin(words: List<Word>, check: ListeningVoiceCheck) {
        val speakable = check.voices
            .filter { it.status == VoiceStatus.READY }
            .mapTo(mutableSetOf()) { it.languageCode }
        playback.start(words, speakable, currentState.selection)
    }

    /** Mirrors the controller into the screen; setup phases are owned here and left alone. */
    private fun onPlayback(state: ListeningPlayback) {
        when (state) {
            is ListeningPlayback.Active -> updateState {
                copy(
                    screen = ListeningScreenState.Active(
                        session = state.session,
                        isPlaying = state.isPlaying,
                        speakableLanguages = state.speakableLanguages,
                        heardWordIds = state.heardWordIds,
                    )
                )
            }
            is ListeningPlayback.Finished -> updateState {
                copy(screen = ListeningScreenState.Finished(state.wordsHeard, state.durationMs))
            }
            ListeningPlayback.Idle -> if (
                currentState.screen is ListeningScreenState.Active ||
                currentState.screen is ListeningScreenState.Finished
            ) {
                updateState { copy(screen = ListeningScreenState.Idle) }
            }
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

    private fun persistSelection(change: ListeningSelection.() -> ListeningSelection) =
        // Start from what the user sees, so a fallback source becomes the saved one once they edit it.
        persist { copy(selection = currentState.selection.change()) }

    private fun updateNeedsVoices(
        reducer: ListeningScreenState.NeedsVoices.() -> ListeningScreenState.NeedsVoices,
    ) {
        val needs = currentState.screen as? ListeningScreenState.NeedsVoices ?: return
        updateState { copy(screen = needs.reducer()) }
    }
}
