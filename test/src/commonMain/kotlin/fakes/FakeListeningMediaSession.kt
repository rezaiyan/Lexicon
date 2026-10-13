package fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import media.IListeningMediaSession
import media.ListeningMediaState
import media.MediaCommand

/** Records what was rendered and lets tests press system media buttons. */
class FakeListeningMediaSession : IListeningMediaSession {
    val rendered = mutableListOf<ListeningMediaState>()
    var releaseCount = 0
        private set

    private val presses = MutableSharedFlow<MediaCommand>(extraBufferCapacity = 16)
    override val commands: Flow<MediaCommand> = presses

    val last: ListeningMediaState? get() = rendered.lastOrNull()

    fun press(command: MediaCommand) {
        presses.tryEmit(command)
    }

    override fun render(state: ListeningMediaState) {
        rendered += state
    }

    override fun release() {
        releaseCount++
    }
}
