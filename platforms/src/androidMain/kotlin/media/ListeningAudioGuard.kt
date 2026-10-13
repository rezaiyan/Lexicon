package media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.content.ContextCompat

/**
 * Audio etiquette for listening mode while it plays, the way a podcast app behaves:
 * - holds audio focus; a call/alarm/navigation prompt pauses it and it resumes afterwards, while
 *   another app starting music pauses it for good. It pauses rather than ducks, since speech under
 *   speech is unintelligible.
 * - pauses when headphones or Bluetooth disconnect, so it never switches to the loudspeaker.
 * - keeps the CPU awake (partial wake lock) for synthesis and timed pauses with the screen off.
 *
 * All of it is held only while actually playing.
 */
internal class ListeningAudioGuard(
    private val context: Context,
    private val onCommand: (MediaCommand) -> Unit,
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val wakeLock = context.getSystemService(PowerManager::class.java)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
        .apply { setReferenceCounted(false) }

    private var playing = false
    private var holdsFocus = false
    private var noisyRegistered = false

    /** Paused by a transient focus loss (call, alarm, navigation): resume when focus returns. */
    private var resumeOnFocusGain = false

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                holdsFocus = false
                onCommand(MediaCommand.Pause)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> if (playing) {
                resumeOnFocusGain = true
                onCommand(MediaCommand.Pause)
            }
            AudioManager.AUDIOFOCUS_GAIN -> if (resumeOnFocusGain) {
                resumeOnFocusGain = false
                onCommand(MediaCommand.Play)
            }
        }
    }

    private val focusRequest: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(focusListener, Handler(Looper.getMainLooper()))
                .build()
        } else {
            null
        }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) onCommand(MediaCommand.Pause)
        }
    }

    /** Called on every render; acquires everything when playback starts and lets go when it stops. */
    fun update(isPlaying: Boolean) {
        if (isPlaying == playing) return
        playing = isPlaying
        if (isPlaying) {
            resumeOnFocusGain = false
            if (!holdsFocus && !requestFocus()) {
                // A call is in progress: don't talk over it.
                playing = false
                onCommand(MediaCommand.Pause)
                return
            }
            registerNoisy()
            wakeLock.acquire(MAX_WAKE_LOCK_MS)
        } else {
            unregisterNoisy()
            if (wakeLock.isHeld) wakeLock.release()
            // Keep focus through a transient loss so the system tells us when to resume.
            if (!resumeOnFocusGain) abandonFocus()
        }
    }

    /** The session is gone: drop everything, including a pending auto-resume. */
    fun release() {
        resumeOnFocusGain = false
        update(false)
        abandonFocus()
    }

    private fun requestFocus(): Boolean {
        val result = if (focusRequest != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        holdsFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return holdsFocus
    }

    private fun abandonFocus() {
        if (!holdsFocus) return
        holdsFocus = false
        if (focusRequest != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
    }

    private fun registerNoisy() {
        if (noisyRegistered) return
        ContextCompat.registerReceiver(
            context,
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        noisyRegistered = true
    }

    private fun unregisterNoisy() {
        if (!noisyRegistered) return
        context.unregisterReceiver(noisyReceiver)
        noisyRegistered = false
    }

    private companion object {
        const val WAKE_LOCK_TAG = "Lexicon:listening"

        /** Safety net only; released as soon as playback pauses or ends. Re-acquired on every play. */
        const val MAX_WAKE_LOCK_MS = 3 * 60 * 60 * 1000L
    }
}
