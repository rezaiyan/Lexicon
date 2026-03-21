package tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class AndroidTtsEngine : ITtsEngine {

    private var offlineTts: OfflineTts? = null
    private var audioTrack: AudioTrack? = null
    private var currentSampleRate: Int = 0

    override suspend fun initialize(modelPath: String, tokensPath: String, dataDir: String) {
        withContext(Dispatchers.IO) {
            release()

            Log.d(TAG, "Initializing TTS engine:")
            Log.d(TAG, "  model: $modelPath")
            Log.d(TAG, "  tokens: $tokensPath")
            Log.d(TAG, "  dataDir: $dataDir")

            val vitsConfig = OfflineTtsVitsModelConfig().apply {
                model = modelPath
                tokens = tokensPath
                this.dataDir = dataDir
            }

            val modelConfig = OfflineTtsModelConfig().apply {
                vits = vitsConfig
            }

            val ttsConfig = OfflineTtsConfig().apply {
                model = modelConfig
            }

            // null AssetManager triggers file-based model loading
            offlineTts = OfflineTts(null, ttsConfig)
            currentSampleRate = offlineTts?.sampleRate() ?: 22050
            Log.d(TAG, "TTS engine initialized. Sample rate: $currentSampleRate")
        }
    }

    override suspend fun synthesizeAndPlay(text: String, speed: Float, speakerId: Int) {
        withContext(Dispatchers.IO) {
            val tts = offlineTts ?: run {
                Log.w(TAG, "synthesizeAndPlay called but TTS not initialized")
                return@withContext
            }

            Log.d(TAG, "Generating audio for: \"$text\" (speed=$speed, sid=$speakerId)")
            val audio = tts.generate(text = text, sid = speakerId, speed = speed)
            if (!coroutineContext.isActive) return@withContext

            val samples = audio.samples
            Log.d(TAG, "Generated ${samples.size} samples at ${currentSampleRate}Hz")
            if (samples.isEmpty()) return@withContext

            playAudio(samples, currentSampleRate)
        }
    }

    private fun playAudio(samples: FloatArray, sampleRate: Int) {
        stopAudioTrack()

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )

        val actualBufferSize = bufferSize.coerceAtLeast(samples.size * 4)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(actualBufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack = track
        track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
        track.play()
        Log.d(TAG, "Audio playback started (${samples.size} samples, buffer=$actualBufferSize)")
    }

    override suspend fun stop() {
        withContext(Dispatchers.IO) {
            stopAudioTrack()
        }
    }

    private fun stopAudioTrack() {
        audioTrack?.let { track ->
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.release()
        }
        audioTrack = null
    }

    override fun release() {
        stopAudioTrack()
        offlineTts?.release()
        offlineTts = null
    }

    override fun isInitialized(): Boolean = offlineTts != null

    override fun numSpeakers(): Int = offlineTts?.numSpeakers() ?: 1

    companion object {
        private const val TAG = "TtsEngine"
    }
}
