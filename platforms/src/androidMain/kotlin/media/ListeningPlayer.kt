package media

import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture

/**
 * Media3 [Player] facade over listening mode's own sequencer. It plays nothing itself: it mirrors
 * the state handed to [show] so the media session can drive the notification, lock screen,
 * Bluetooth/headset and Wear/Auto, and turns their presses into [MediaCommand]s via [onCommand].
 *
 * Every word of the queue is a playlist item, so "next"/"previous" availability and the queue
 * position are native. Must be used on the main looper.
 */
internal class ListeningPlayer(
    private val onCommand: (MediaCommand) -> Unit,
) : SimpleBasePlayer(Looper.getMainLooper()) {

    var current: ListeningMediaState? = null
        private set

    private var artwork: ByteArray? = null
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Completes once the sequencer answers a press with its next state, so controllers keep showing
     * the optimistic result (e.g. "paused") instead of flickering back to the old one meanwhile.
     */
    private var pending: SettableFuture<Unit>? = null

    /** Publishes [state] (null = nothing to show) to the session and every connected controller. */
    fun show(state: ListeningMediaState?, artworkData: ByteArray?) {
        current = state
        artwork = artworkData
        settlePending()
        invalidateState()
    }

    val isFinished: Boolean get() = current is ListeningMediaState.Finished

    override fun getState(): State {
        val builder = State.Builder().setAudioAttributes(SPEECH_ATTRIBUTES)
        return when (val state = current) {
            null -> builder
                .setAvailableCommands(Player.Commands.EMPTY)
                .setPlaybackState(Player.STATE_IDLE)
                .build()

            is ListeningMediaState.Playing -> builder
                .setAvailableCommands(PLAYING_COMMANDS)
                .setPlaylist(playlist(state))
                .setCurrentMediaItemIndex(state.index)
                .setPlayWhenReady(state.isPlaying, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                .setPlaybackState(Player.STATE_READY)
                .build()

            is ListeningMediaState.Finished -> builder
                .setAvailableCommands(FINISHED_COMMANDS)
                .setPlaylist(
                    listOf(
                        item(
                            uid = FINISHED_UID,
                            title = state.title,
                            artist = state.subtitle,
                            album = null,
                        )
                    )
                )
                .setPlayWhenReady(false, Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM)
                .setPlaybackState(Player.STATE_ENDED)
                .build()
        }
    }

    /**
     * One item per word. Only the current one carries the live text; the others are placeholders
     * the system never shows, but their count makes next/previous and "3 of 20" native.
     */
    private fun playlist(state: ListeningMediaState.Playing): List<MediaItemData> =
        List(state.total) { index ->
            if (index == state.index) {
                item(
                    uid = index,
                    title = state.title,
                    // Before the answer is spoken there is nothing to spoil: show where we are instead.
                    artist = state.subtitle ?: state.detail,
                    album = state.detail,
                )
            } else {
                MediaItemData.Builder(index).setDurationUs(C.TIME_UNSET).setIsSeekable(false).build()
            }
        }

    private fun item(uid: Any, title: String, artist: String, album: String?): MediaItemData =
        MediaItemData.Builder(uid)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setDisplayTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
                    .setIsPlayable(true)
                    .build()
            )
            .setDurationUs(C.TIME_UNSET)
            .setIsSeekable(false)
            .build()

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        // Play on a finished session (lock screen, headset, Assistant) means "play it again".
        return when {
            isFinished -> if (playWhenReady) send(MediaCommand.Repeat) else done()
            playWhenReady -> send(MediaCommand.Play)
            else -> send(MediaCommand.Pause)
        }
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        return when (seekCommand) {
            Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> send(MediaCommand.Next)
            // The sequencer applies the music-player rule (restart vs. previous word) itself.
            Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM ->
                send(MediaCommand.Previous)
            else -> done()
        }
    }

    override fun handleStop(): ListenableFuture<*> =
        send(if (isFinished) MediaCommand.Close else MediaCommand.Stop)

    override fun handlePrepare(): ListenableFuture<*> = done()

    override fun handleRelease(): ListenableFuture<*> = done()

    private fun done(): ListenableFuture<*> = Futures.immediateVoidFuture()

    /** Sends [command] and waits for the resulting state, or [PENDING_TIMEOUT_MS] if nothing changes. */
    private fun send(command: MediaCommand): ListenableFuture<*> {
        settlePending()
        val future = SettableFuture.create<Unit>()
        pending = future
        handler.postDelayed({ if (pending === future) settlePending() }, PENDING_TIMEOUT_MS)
        onCommand(command)
        return future
    }

    private fun settlePending() {
        pending?.set(Unit)
        pending = null
    }

    companion object {
        private const val FINISHED_UID = "finished"
        private const val PENDING_TIMEOUT_MS = 500L

        private val SPEECH_ATTRIBUTES = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        private val PLAYING_COMMANDS = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_STOP,
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_TIMELINE,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_GET_AUDIO_ATTRIBUTES,
                Player.COMMAND_RELEASE,
            )
            .build()

        // No play/pause or seeking once finished: the card offers exactly Repeat and Close.
        private val FINISHED_COMMANDS = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_STOP,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_TIMELINE,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_GET_AUDIO_ATTRIBUTES,
                Player.COMMAND_RELEASE,
            )
            .build()
    }
}
