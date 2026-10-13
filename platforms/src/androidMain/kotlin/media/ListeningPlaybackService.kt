package media

import android.content.Intent
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.alirezaiyan.vokab.platforms.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Hosts listening mode's media session. Media3 runs it as a `mediaPlayback` foreground service while
 * playing, keeps it foreground for a while after a pause, and drives the media notification, lock
 * screen, Bluetooth/headset, Wear and Auto controls from [ListeningPlayer].
 */
class ListeningPlaybackService : MediaSessionService(), KoinComponent {

    private val owner: IListeningMediaSession by inject()
    private var session: MediaSession? = null

    private val player: ListeningPlayer? get() = (owner as? AndroidListeningMediaSession)?.player

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            // Released by the app (session closed): nothing left to show, so go away.
            if (player.playbackState == Player.STATE_IDLE) stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        val androidOwner = owner as? AndroidListeningMediaSession ?: run { stopSelf(); return }
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setChannelName(R.string.listening_notification_channel)
                .setNotificationId(NOTIFICATION_ID)
                .build()
                .apply { setSmallIcon(R.drawable.ic_stat_listening) }
        )
        // Once closed (player idle) the notification disappears; finished sessions stay (ENDED).
        setShowNotificationForIdlePlayer(SHOW_NOTIFICATION_FOR_IDLE_PLAYER_NEVER)
        session = MediaSession.Builder(this, androidOwner.player)
            .setId(SESSION_ID)
            .apply { androidOwner.openAppIntent()?.let(::setSessionActivity) }
            .setCallback(Callback())
            .setMediaButtonPreferences(buttons())
            .build()
        androidOwner.player.addListener(playerListener)
        // Paused: Media3 keeps the service foreground for a grace period so play resumes instantly.
        // Finished: leave the foreground at once so the card can be swiped away.
        androidOwner.onFinishedChanged = { finished ->
            setForegroundServiceTimeoutMs(if (finished) 0L else DEFAULT_FOREGROUND_SERVICE_TIMEOUT_MS)
        }
        androidOwner.onShown = ::refreshButtons
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        player?.removeListener(playerListener)
        (owner as? AndroidListeningMediaSession)?.apply {
            onFinishedChanged = null
            onShown = null
        }
        // The player belongs to the app-wide session owner and outlives this service.
        session?.release()
        session = null
        super.onDestroy()
    }

    private fun refreshButtons() {
        session?.setMediaButtonPreferences(buttons())
    }

    /**
     * Playing: just previous / play-pause / next, like any podcast player. Paused: a Close in the free
     * slot so the session can be ended without reopening the app. Finished: the big center play button
     * repeats (the player maps it), with Close beside it.
     */
    private fun buttons(): List<CommandButton> {
        val state = player?.current ?: return emptyList()
        val labels = state.labels
        return when (state) {
            is ListeningMediaState.Playing ->
                if (state.isPlaying) emptyList()
                else listOf(closeButton(labels.stop, STOP, CommandButton.SLOT_OVERFLOW))
            is ListeningMediaState.Finished -> listOf(
                closeButton(labels.close, CLOSE, CommandButton.SLOT_FORWARD, CommandButton.SLOT_OVERFLOW),
            )
        }
    }

    private fun closeButton(label: String, command: SessionCommand, vararg slots: Int) =
        CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setCustomIconResId(R.drawable.ic_listening_close)
            .setDisplayName(label)
            .setSessionCommand(command)
            .setSlots(*slots)
            .build()

    private inner class Callback : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult = MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(STOP)
                    .add(REPEAT)
                    .add(CLOSE)
                    .build()
            )
            .setMediaButtonPreferences(buttons())
            .build()

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            val command = when (customCommand.customAction) {
                STOP.customAction -> MediaCommand.Stop
                REPEAT.customAction -> MediaCommand.Repeat
                CLOSE.customAction -> MediaCommand.Close
                else -> return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
            }
            (owner as? AndroidListeningMediaSession)?.dispatch(command)
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onMediaButtonEvent(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            intent: Intent,
        ): Boolean {
            // A swiped-away notification (finished, or paused long enough to leave the foreground)
            // ends the session, so nothing lingers. Media3 still handles the dismissal itself.
            if (intent.getBooleanExtra(MediaNotification.NOTIFICATION_DISMISSED_EVENT_KEY, false)) {
                val finished = player?.isFinished == true
                (owner as? AndroidListeningMediaSession)?.dispatch(
                    if (finished) MediaCommand.Close else MediaCommand.Stop
                )
            }
            return false
        }
    }

    private companion object {
        const val SESSION_ID = "listening"
        const val CHANNEL_ID = "listening_playback"
        const val NOTIFICATION_ID = 4_201
        val STOP = SessionCommand("media.listening.STOP", Bundle.EMPTY)
        val REPEAT = SessionCommand("media.listening.REPEAT", Bundle.EMPTY)
        val CLOSE = SessionCommand("media.listening.CLOSE", Bundle.EMPTY)
    }
}
