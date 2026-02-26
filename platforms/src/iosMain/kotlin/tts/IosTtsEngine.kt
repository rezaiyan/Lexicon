package tts

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSLog
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import sherpa_onnx.SherpaOnnxCreateOfflineTts
import sherpa_onnx.SherpaOnnxDestroyOfflineTts
import sherpa_onnx.SherpaOnnxDestroyOfflineTtsGeneratedAudio
import sherpa_onnx.SherpaOnnxOfflineTtsConfig
import sherpa_onnx.SherpaOnnxOfflineTtsGenerate
import sherpa_onnx.SherpaOnnxOfflineTtsSampleRate
import sherpa_onnx.SherpaOnnxWriteWave
import cnames.structs.SherpaOnnxOfflineTts
import kotlin.coroutines.coroutineContext

class IosTtsEngine : ITtsEngine {

    private var ttsPtr: CPointer<SherpaOnnxOfflineTts>? = null
    private var currentSampleRate: Int = 0
    private var audioPlayer: AVAudioPlayer? = null

    override suspend fun initialize(modelPath: String, tokensPath: String, dataDir: String) {
        withContext(Dispatchers.IO) {
            release()

            NSLog("IosTtsEngine: Initializing with model=$modelPath tokens=$tokensPath dataDir=$dataDir")

            memScoped {
                val config = alloc<SherpaOnnxOfflineTtsConfig>()
                config.model.vits.model = modelPath.cstr.ptr
                config.model.vits.tokens = tokensPath.cstr.ptr
                config.model.vits.data_dir = dataDir.cstr.ptr
                config.model.vits.noise_scale = 0.667f
                config.model.vits.noise_scale_w = 0.8f
                config.model.vits.length_scale = 1.0f
                config.model.num_threads = 2
                config.model.debug = 1
                config.model.provider = "cpu".cstr.ptr

                ttsPtr = SherpaOnnxCreateOfflineTts(config.ptr)
            }

            if (ttsPtr != null) {
                currentSampleRate = SherpaOnnxOfflineTtsSampleRate(ttsPtr).toInt()
                NSLog("IosTtsEngine: Initialized. Sample rate: $currentSampleRate")
            } else {
                NSLog("IosTtsEngine: Failed to create TTS engine")
            }
        }
    }

    override suspend fun synthesizeAndPlay(text: String) {
        withContext(Dispatchers.IO) {
            val tts = ttsPtr ?: run {
                NSLog("IosTtsEngine: synthesizeAndPlay called but not initialized")
                return@withContext
            }

            NSLog("IosTtsEngine: Generating audio for: \"$text\"")
            val audio = SherpaOnnxOfflineTtsGenerate(tts, text, 0, 1.0f) ?: run {
                NSLog("IosTtsEngine: Generate returned null")
                return@withContext
            }

            if (!coroutineContext.isActive) {
                SherpaOnnxDestroyOfflineTtsGeneratedAudio(audio)
                return@withContext
            }

            val numSamples = audio.pointed.n
            val sampleRate = audio.pointed.sample_rate
            val samplesPtr = audio.pointed.samples

            NSLog("IosTtsEngine: Generated $numSamples samples at ${sampleRate}Hz")

            if (numSamples > 0 && samplesPtr != null) {
                val wavPath = "${NSTemporaryDirectory()}tts_output.wav"
                SherpaOnnxWriteWave(samplesPtr, numSamples, sampleRate, wavPath)

                SherpaOnnxDestroyOfflineTtsGeneratedAudio(audio)

                playWavFile(wavPath)
            } else {
                SherpaOnnxDestroyOfflineTtsGeneratedAudio(audio)
            }
        }
    }

    private suspend fun playWavFile(path: String) {
        withContext(Dispatchers.Main) {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, null)
            session.setActive(true, null)

            val url = NSURL.fileURLWithPath(path)
            val player = AVAudioPlayer(contentsOfURL = url, error = null) ?: run {
                NSLog("IosTtsEngine: Could not create AVAudioPlayer for $path")
                return@withContext
            }

            audioPlayer = player
            player.play()
        }

        // Wait for playback to complete
        while (audioPlayer?.playing == true) {
            delay(100)
        }
    }

    override suspend fun stop() {
        withContext(Dispatchers.Main) {
            audioPlayer?.stop()
            audioPlayer = null
        }
    }

    override fun release() {
        audioPlayer?.stop()
        audioPlayer = null
        ttsPtr?.let { SherpaOnnxDestroyOfflineTts(it) }
        ttsPtr = null
    }

    override fun isInitialized(): Boolean = ttsPtr != null
}
