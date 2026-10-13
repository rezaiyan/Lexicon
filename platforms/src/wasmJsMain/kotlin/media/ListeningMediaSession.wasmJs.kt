package media

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Listening mode is mobile-only; the web has no on-device TTS. */
actual fun createListeningMediaSession(): IListeningMediaSession = object : IListeningMediaSession {
    override val commands: Flow<MediaCommand> = emptyFlow()
    override fun render(state: ListeningMediaState) = Unit
    override fun release() = Unit
}
