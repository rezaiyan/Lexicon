package feature.study.listening

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import domain.listening.model.ListeningOrder
import domain.listening.usecase.ObserveListeningSettingsUseCase
import feature.study.listening.playback.ListeningPlayback
import feature.study.listening.playback.ListeningPlaybackController
import feature.study.listening.playback.ListeningPlayerVisibility
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

sealed interface MiniPlayerState {
    data object Hidden : MiniPlayerState

    data class Playing(
        val title: String,
        /** The answer once spoken; null before, so nothing is spoiled. */
        val subtitle: String?,
        /** Share of the queue reached, 0..1. */
        val progress: Float,
        val isPlaying: Boolean,
    ) : MiniPlayerState

    data class Finished(val wordsHeard: Int) : MiniPlayerState
}

/**
 * The compact player above the bottom bar: a running or just-finished listening session while
 * the full player is collapsed, so the learner can browse the app and keep listening.
 */
class ListeningMiniPlayerViewModel(
    private val playback: ListeningPlaybackController,
    visibility: ListeningPlayerVisibility,
    observeSettings: ObserveListeningSettingsUseCase,
) : BaseViewModel<MiniPlayerState, Nothing>() {

    override fun initialState(): MiniPlayerState = MiniPlayerState.Hidden

    init {
        val order = observeSettings(Unit)
            .map { it.order }
            .onStart { emit(ListeningOrder.WORD_FIRST) }
            .catch { emit(ListeningOrder.WORD_FIRST) }
        combine(playback.playback, visibility.isOpen, order) { state, open, currentOrder ->
            if (open) MiniPlayerState.Hidden else state.toMiniPlayer(currentOrder)
        }
            .onEach { mini -> updateState { mini } }
            .launchIn(viewModelScope)
    }

    fun togglePlayback() = playback.togglePlayback()

    fun repeat() = playback.repeat()

    fun close() = playback.close()

    private fun ListeningPlayback.toMiniPlayer(order: ListeningOrder): MiniPlayerState = when (this) {
        ListeningPlayback.Idle -> MiniPlayerState.Hidden
        is ListeningPlayback.Active -> MiniPlayerState.Playing(
            title = session.prompt(order).text,
            subtitle = session.answer(order).text.takeIf { session.isAnswerRevealed },
            progress = (session.index + 1).toFloat() / session.words.size,
            isPlaying = isPlaying,
        )
        is ListeningPlayback.Finished -> MiniPlayerState.Finished(wordsHeard)
    }
}
