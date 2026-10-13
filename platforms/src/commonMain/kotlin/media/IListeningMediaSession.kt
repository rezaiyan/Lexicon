package media

import kotlinx.coroutines.flow.Flow

/**
 * The system's media surface for listening mode: the Android media notification + lock-screen
 * session, or iOS Now Playing (lock screen, Control Center, Dynamic Island) + remote commands.
 *
 * A dumb renderer: it shows what [render] hands it and reports button presses on [commands].
 * It knows nothing about words or sessions, and owns the background-audio keep-alive while playing.
 */
interface IListeningMediaSession {

    /** Presses from the notification, lock screen, headset or Bluetooth, and system pauses (calls, unplug). */
    val commands: Flow<MediaCommand>

    /** Shows [state]; idempotent. The first [ListeningMediaState.Playing] starts the session. */
    fun render(state: ListeningMediaState)

    /** Removes the notification / Now Playing and lets go of background audio. */
    fun release()
}

sealed interface ListeningMediaState {
    val labels: MediaLabels

    /** A session is running (playing or paused); not dismissable while playing. */
    data class Playing(
        /** The prompt being learned, e.g. the word. */
        val title: String,
        /** The answer, once it has been spoken; null before, so nothing is spoiled. */
        val subtitle: String?,
        /** Context line, e.g. "3 of 20 · German → English". */
        val detail: String,
        /** 0-based word index in the queue, and queue size: drives queue position and next/previous. */
        val index: Int,
        val total: Int,
        val isPlaying: Boolean,
        override val labels: MediaLabels,
    ) : ListeningMediaState

    /** The session ended: dismissable, offers Repeat and Close. */
    data class Finished(
        val title: String,
        val subtitle: String,
        override val labels: MediaLabels,
    ) : ListeningMediaState
}

/**
 * Localized labels for the actions the system doesn't name itself (play/pause/next/previous are
 * localized by the platform); resolved by the caller.
 */
data class MediaLabels(
    val stop: String,
    val repeat: String,
    val close: String,
)

enum class MediaCommand {
    Play,
    Pause,
    TogglePlayPause,
    Next,
    Previous,

    /** Ends the running session from the system surface. */
    Stop,

    /** Plays the finished session again. */
    Repeat,

    /** Dismisses the finished session (Close button or swipe). */
    Close,
}

/** Notification `type` extra / userInfo of listening taps; routed to the listening player. */
const val LISTENING_NOTIFICATION_TYPE = "listening"

expect fun createListeningMediaSession(): IListeningMediaSession
