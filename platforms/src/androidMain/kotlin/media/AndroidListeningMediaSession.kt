package media

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.alirezaiyan.vokab.platforms.R
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import notification.openAppPendingIntent
import java.io.ByteArrayOutputStream

/**
 * Android media surface for listening mode, built on Media3: [ListeningPlayer] mirrors the session
 * into a [ListeningPlaybackService], which Media3 runs as a foreground service with the system media
 * notification. Holding a [MediaController] connection keeps the service bound while a session exists.
 * Called on the main thread.
 */
internal class AndroidListeningMediaSession(private val context: Context) : IListeningMediaSession {

    private val _commands = MutableSharedFlow<MediaCommand>(extraBufferCapacity = COMMAND_BUFFER)
    override val commands: Flow<MediaCommand> = _commands.asSharedFlow()

    val player = ListeningPlayer(::dispatch)

    private val audio = ListeningAudioGuard(context, ::dispatch)
    private val handler = Handler(Looper.getMainLooper())

    /** A finished card the user never closed (a carousel swipe only hides it) goes away on its own. */
    private val expireFinished = Runnable { if (player.isFinished) release() }
    private var connection: ListenableFuture<MediaController>? = null
    private val artwork: ByteArray by lazy { renderArtwork() }

    /**
     * Told whether the session has finished, before the player shows it, so the service can leave the
     * foreground right away (dismissable "finished" card) rather than after the pause grace period.
     */
    var onFinishedChanged: ((Boolean) -> Unit)? = null

    fun dispatch(command: MediaCommand) {
        _commands.tryEmit(command)
    }

    override fun render(state: ListeningMediaState) {
        val finished = state is ListeningMediaState.Finished
        if (finished && player.current is ListeningMediaState.Playing) playChime()
        onFinishedChanged?.invoke(finished)
        handler.removeCallbacks(expireFinished)
        if (finished) handler.postDelayed(expireFinished, FINISHED_TTL_MS)
        player.show(state, artwork)
        if (connection == null) connect()
        audio.update(state is ListeningMediaState.Playing && state.isPlaying)
    }

    override fun release() {
        handler.removeCallbacks(expireFinished)
        audio.release()
        player.show(null, null)
        connection?.let(MediaController::releaseFuture)
        connection = null
    }

    /** Tapping the notification or lock-screen card reopens the listening player. */
    fun openAppIntent(): PendingIntent? = context.openAppPendingIntent(OPEN_REQUEST_CODE, LISTENING_NOTIFICATION_TYPE)

    private fun playChime() {
        val samples = EndChime.samples()
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(EndChime.SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * Float.SIZE_BYTES)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
        track.setNotificationMarkerPosition(samples.size)
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack) = track.release()
            override fun onPeriodicNotification(track: AudioTrack) = Unit
        })
        track.play()
    }

    private fun connect() {
        val token = SessionToken(context, ComponentName(context, ListeningPlaybackService::class.java))
        connection = MediaController.Builder(context, token).buildAsync()
    }

    /** Brand gradient with a headphones glyph, so the lock screen and media card never show a grey tile. */
    private fun renderArtwork(): ByteArray {
        val bitmap = Bitmap.createBitmap(ARTWORK_PX, ARTWORK_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val size = ARTWORK_PX.toFloat()
        canvas.drawRect(
            0f, 0f, size, size,
            Paint().apply {
                shader = LinearGradient(0f, 0f, size, size, BRAND_PURPLE, BRAND_GREEN, Shader.TileMode.CLAMP)
            },
        )
        ContextCompat.getDrawable(context, R.drawable.ic_stat_listening)?.let { glyph ->
            val inset = (ARTWORK_PX * GLYPH_INSET_RATIO).toInt()
            glyph.setBounds(inset, inset, ARTWORK_PX - inset, ARTWORK_PX - inset)
            glyph.draw(canvas)
        }
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }

    private companion object {
        const val COMMAND_BUFFER = 16
        const val FINISHED_TTL_MS = 30 * 60 * 1000L
        const val OPEN_REQUEST_CODE = 4_202
        const val ARTWORK_PX = 512
        const val GLYPH_INSET_RATIO = 0.28f
        const val PNG_QUALITY = 100
        const val BRAND_PURPLE = 0xFF7F5AF0.toInt()
        const val BRAND_GREEN = 0xFF2CB67D.toInt()
    }
}
