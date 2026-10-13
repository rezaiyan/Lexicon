package feature.study.listening.playback

import domain.listening.model.ListeningOrder
import domain.listening.usecase.ObserveListeningSettingsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import media.IListeningMediaSession
import media.ListeningMediaState
import media.MediaCommand

/**
 * Connects listening playback to the system media surface: renders every visible change of
 * [controller] on [session], and turns notification / lock-screen / headset presses into commands.
 */
class ListeningMediaBridge(
    private val controller: ListeningPlaybackController,
    private val session: IListeningMediaSession,
    private val observeSettings: ObserveListeningSettingsUseCase,
    private val loadText: suspend () -> ListeningMediaText,
    private val scope: CoroutineScope,
) {
    private var text: ListeningMediaText? = null
    private var shown = false

    fun start() {
        val order = observeSettings(Unit)
            .map { it.order }
            .catch { emit(ListeningOrder.WORD_FIRST) }
            .onStart { emit(ListeningOrder.WORD_FIRST) }
            .distinctUntilChanged()

        combine(controller.playback, order) { playback, currentOrder ->
            if (playback == ListeningPlayback.Idle) null else playback.toMediaState(currentOrder, text())
        }
            .distinctUntilChanged()
            .onEach(::show)
            .launchIn(scope)

        session.commands
            .onEach(::handle)
            .launchIn(scope)
    }

    private fun show(state: ListeningMediaState?) {
        if (state != null) {
            shown = true
            session.render(state)
        } else if (shown) {
            shown = false
            // Re-resolved next session, so an in-app language change shows up there.
            text = null
            session.release()
        }
    }

    private fun handle(command: MediaCommand) {
        when (command) {
            MediaCommand.Play -> controller.resume()
            MediaCommand.Pause -> controller.pause()
            MediaCommand.TogglePlayPause -> controller.togglePlayback()
            MediaCommand.Next -> controller.next()
            MediaCommand.Previous -> controller.previous()
            MediaCommand.Repeat -> controller.repeat()
            MediaCommand.Stop, MediaCommand.Close -> controller.close()
        }
    }

    private suspend fun text(): ListeningMediaText = text ?: loadText().also { text = it }
}
