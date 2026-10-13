package media

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.MediaPlayer.MPMediaItemPropertyAlbumTitle
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyArtwork
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoMediaTypeAudio
import platform.MediaPlayer.MPNowPlayingInfoPropertyMediaType
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackQueueCount
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackQueueIndex
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommand
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationState
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionProvisional
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS media surface for listening mode: Now Playing (lock screen, Control Center, Dynamic Island)
 * and remote commands (headphones, CarPlay, Watch), with the audio side in [IosListeningAudio].
 * A finished session stays in Now Playing (play = repeat) and, when the app is in the background,
 * posts a local notification with Repeat / Close.
 *
 * Called on the main thread.
 */
class IosListeningMediaSession : IListeningMediaSession {

    private val _commands = MutableSharedFlow<MediaCommand>(extraBufferCapacity = COMMAND_BUFFER)
    override val commands: Flow<MediaCommand> = _commands.asSharedFlow()

    private var current: ListeningMediaState? = null
    private var active = false
    private var commandTargets: List<Pair<MPRemoteCommand, Any>> = emptyList()

    private val audio = IosListeningAudio(
        onCommand = ::dispatch,
        isPlaying = { (current as? ListeningMediaState.Playing)?.isPlaying == true },
    )
    private val artwork by lazy { listeningArtwork() }

    private val isFinished: Boolean get() = current is ListeningMediaState.Finished

    private fun dispatch(command: MediaCommand) {
        _commands.tryEmit(command)
    }

    override fun render(state: ListeningMediaState) {
        val previous = current
        current = state
        if (!active) {
            active = true
            audio.activate()
            registerRemoteCommands()
        }
        when (state) {
            is ListeningMediaState.Playing -> {
                audio.setKeepAlive(state.isPlaying)
                updateCommands(finished = false)
                removeFinishedNotification()
            }
            is ListeningMediaState.Finished -> {
                audio.setKeepAlive(false)
                updateCommands(finished = true)
                if (previous is ListeningMediaState.Playing) {
                    audio.playChime()
                    if (isInBackground()) postFinishedNotification(state)
                }
            }
        }
        publishNowPlaying(state)
    }

    override fun release() {
        current = null
        if (!active) return
        active = false
        commandTargets.forEach { (command, target) ->
            command.removeTarget(target)
            command.enabled = false
        }
        commandTargets = emptyList()
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
        removeFinishedNotification()
        audio.deactivate()
    }

    /** Repeat / Close / swipe from the "finished" notification (routed from the app delegate). */
    fun handleNotificationAction(actionId: String) {
        when (actionId) {
            ACTION_REPEAT -> dispatch(MediaCommand.Repeat)
            ACTION_CLOSE -> dispatch(MediaCommand.Close)
        }
    }

    private fun publishNowPlaying(state: ListeningMediaState) {
        val info: Map<Any?, Any?> = when (state) {
            is ListeningMediaState.Playing -> mapOf(
                MPMediaItemPropertyTitle to state.title,
                // Before the answer is spoken there's nothing to spoil: show where we are instead.
                MPMediaItemPropertyArtist to (state.subtitle ?: state.detail),
                MPMediaItemPropertyAlbumTitle to state.detail,
                MPMediaItemPropertyArtwork to artwork,
                MPNowPlayingInfoPropertyPlaybackRate to if (state.isPlaying) 1.0 else 0.0,
                MPNowPlayingInfoPropertyPlaybackQueueIndex to state.index,
                MPNowPlayingInfoPropertyPlaybackQueueCount to state.total,
                MPNowPlayingInfoPropertyMediaType to MPNowPlayingInfoMediaTypeAudio,
            )
            is ListeningMediaState.Finished -> mapOf(
                MPMediaItemPropertyTitle to state.title,
                MPMediaItemPropertyArtist to state.subtitle,
                MPMediaItemPropertyArtwork to artwork,
                MPNowPlayingInfoPropertyPlaybackRate to 0.0,
                MPNowPlayingInfoPropertyMediaType to MPNowPlayingInfoMediaTypeAudio,
            )
        }
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = info
    }

    private fun registerRemoteCommands() {
        val center = MPRemoteCommandCenter.sharedCommandCenter()
        fun MPRemoteCommand.on(action: () -> Unit): Pair<MPRemoteCommand, Any> {
            val target = addTargetWithHandler { _ ->
                action()
                MPRemoteCommandHandlerStatusSuccess
            }
            return this to target
        }
        // Play on a finished session (lock screen, AirPods, Siri) means "play it again".
        commandTargets = listOf(
            center.playCommand.on { dispatch(if (isFinished) MediaCommand.Repeat else MediaCommand.Play) },
            center.pauseCommand.on { dispatch(MediaCommand.Pause) },
            center.togglePlayPauseCommand.on {
                dispatch(if (isFinished) MediaCommand.Repeat else MediaCommand.TogglePlayPause)
            },
            center.nextTrackCommand.on { dispatch(MediaCommand.Next) },
            center.previousTrackCommand.on { dispatch(MediaCommand.Previous) },
            center.stopCommand.on { dispatch(if (isFinished) MediaCommand.Close else MediaCommand.Stop) },
        )
        // No scrubbing or skip intervals: words aren't a timeline.
        listOf(
            center.changePlaybackPositionCommand,
            center.skipForwardCommand,
            center.skipBackwardCommand,
            center.seekForwardCommand,
            center.seekBackwardCommand,
        ).forEach { it.enabled = false }
    }

    private fun updateCommands(finished: Boolean) {
        val center = MPRemoteCommandCenter.sharedCommandCenter()
        center.playCommand.enabled = true
        center.togglePlayPauseCommand.enabled = true
        center.stopCommand.enabled = true
        center.pauseCommand.enabled = !finished
        center.nextTrackCommand.enabled = !finished
        center.previousTrackCommand.enabled = !finished
    }

    private fun isInBackground(): Boolean =
        UIApplication.sharedApplication.applicationState != UIApplicationState.UIApplicationStateActive

    private fun postFinishedNotification(state: ListeningMediaState.Finished) {
        val content = UNMutableNotificationContent().apply {
            setTitle(state.title)
            setBody(state.subtitle)
            setCategoryIdentifier(FINISHED_CATEGORY)
            setThreadIdentifier(FINISHED_ID)
            setUserInfo(mapOf<Any?, Any?>("type" to LISTENING_NOTIFICATION_TYPE))
        }
        val request = UNNotificationRequest.requestWithIdentifier(FINISHED_ID, content, null)
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.getNotificationSettingsWithCompletionHandler { settings ->
            when (settings?.authorizationStatus) {
                UNAuthorizationStatusAuthorized,
                UNAuthorizationStatusProvisional,
                UNAuthorizationStatusEphemeral -> center.addNotificationRequest(request, null)
                // Never asked: deliver quietly to Notification Center, no prompt mid-session.
                // A later full permission request still shows the system prompt.
                UNAuthorizationStatusNotDetermined -> center.requestAuthorizationWithOptions(
                    UNAuthorizationOptionProvisional or UNAuthorizationOptionAlert,
                ) { granted, _ -> if (granted) center.addNotificationRequest(request, null) }
                else -> Unit // Denied: Now Playing still offers Repeat.
            }
        }
    }

    private fun removeFinishedNotification() {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.removeDeliveredNotificationsWithIdentifiers(listOf(FINISHED_ID))
        center.removePendingNotificationRequestsWithIdentifiers(listOf(FINISHED_ID))
    }

    companion object {
        /** Category of the "session finished" local notification; registered by the app delegate. */
        const val FINISHED_CATEGORY = "LISTENING_FINISHED"
        const val ACTION_REPEAT = "LISTENING_REPEAT"
        const val ACTION_CLOSE = "LISTENING_CLOSE"
        private const val FINISHED_ID = "listening-finished"
        private const val COMMAND_BUFFER = 16
    }
}
