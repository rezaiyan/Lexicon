package media

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioEngineConfigurationChangeNotification
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioPlayerNodeBufferLoops
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionOptionKey
import platform.AVFAudio.AVAudioSessionInterruptionOptionShouldResume
import platform.AVFAudio.AVAudioSessionInterruptionTypeBegan
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVAudioSessionModeSpokenAudio
import platform.AVFAudio.AVAudioSessionRouteChangeNotification
import platform.AVFAudio.AVAudioSessionRouteChangeReasonKey
import platform.AVFAudio.AVAudioSessionRouteChangeReasonOldDeviceUnavailable
import platform.AVFAudio.AVAudioSessionRouteSharingPolicyLongFormAudio
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.darwin.NSObjectProtocol

/**
 * Listening mode's audio on iOS:
 * - an audio session for long-form spoken audio (behaves like a podcast: other audio pauses,
 *   AirPlay treats it as long-form), held for the whole session;
 * - a silent keep-alive output while playing, so the timed pauses and speech synthesis between
 *   words don't count as "not playing" and iOS doesn't suspend the app in the background;
 * - interruptions (calls, Siri, alarms) pause and resume; unplugged headphones pause;
 * - the end chime.
 *
 * Called on the main thread.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosListeningAudio(
    private val onCommand: (MediaCommand) -> Unit,
    private val isPlaying: () -> Boolean,
) {
    private val audioSession = AVAudioSession.sharedInstance()
    private val engine = AVAudioEngine()
    private val keepAliveNode = AVAudioPlayerNode()
    private val chimeNode = AVAudioPlayerNode()
    private val keepAliveFormat = AVAudioFormat(standardFormatWithSampleRate = KEEP_ALIVE_RATE, channels = 1u)
    private val chimeFormat =
        AVAudioFormat(standardFormatWithSampleRate = EndChime.SAMPLE_RATE.toDouble(), channels = 1u)

    private var engineWired = false
    private var keepAlive = false
    private var resumeAfterInterruption = false
    private var observers: List<NSObjectProtocol> = emptyList()

    fun activate() {
        audioSession.setCategory(
            AVAudioSessionCategoryPlayback,
            mode = AVAudioSessionModeSpokenAudio,
            routeSharingPolicy = AVAudioSessionRouteSharingPolicyLongFormAudio,
            options = 0u,
            error = null,
        )
        audioSession.setActive(true, null)
        wireEngine()
        observeSystemEvents()
    }

    fun deactivate() {
        setKeepAlive(false)
        engine.stop()
        observers.forEach(NSNotificationCenter.defaultCenter::removeObserver)
        observers = emptyList()
        resumeAfterInterruption = false
        audioSession.setActive(false, AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, null)
    }

    /** Silent looping output while playing: keeps the app running through pauses and synthesis. */
    fun setKeepAlive(enabled: Boolean) {
        if (enabled == keepAlive) return
        keepAlive = enabled
        if (enabled) {
            startEngine()
            val buffer = AVAudioPCMBuffer(pCMFormat = keepAliveFormat, frameCapacity = KEEP_ALIVE_FRAMES)
            buffer.frameLength = KEEP_ALIVE_FRAMES
            buffer.floatChannelData?.get(0)?.let { channel ->
                for (i in 0 until KEEP_ALIVE_FRAMES.toInt()) channel[i] = 0f
            }
            keepAliveNode.scheduleBuffer(
                buffer,
                atTime = null,
                options = AVAudioPlayerNodeBufferLoops,
                completionHandler = null,
            )
            keepAliveNode.play()
        } else {
            keepAliveNode.stop()
        }
    }

    fun playChime() {
        val samples = EndChime.samples()
        val buffer = AVAudioPCMBuffer(pCMFormat = chimeFormat, frameCapacity = samples.size.toUInt())
        buffer.frameLength = samples.size.toUInt()
        val channel = buffer.floatChannelData?.get(0) ?: return
        samples.forEachIndexed { i, sample -> channel[i] = sample }
        startEngine()
        chimeNode.scheduleBuffer(buffer, completionHandler = null)
        chimeNode.play()
    }

    private fun wireEngine() {
        if (engineWired) return
        engineWired = true
        engine.attachNode(keepAliveNode)
        engine.attachNode(chimeNode)
        engine.connect(keepAliveNode, to = engine.mainMixerNode, format = keepAliveFormat)
        engine.connect(chimeNode, to = engine.mainMixerNode, format = chimeFormat)
    }

    private fun startEngine() {
        if (!engine.running) engine.startAndReturnError(null)
    }

    private fun observeSystemEvents() {
        if (observers.isNotEmpty()) return
        val center = NSNotificationCenter.defaultCenter
        val queue = NSOperationQueue.mainQueue
        observers = listOf(
            center.addObserverForName(AVAudioSessionInterruptionNotification, audioSession, queue, ::onInterruption),
            center.addObserverForName(AVAudioSessionRouteChangeNotification, audioSession, queue, ::onRouteChange),
            // A route or format change stops the engine; bring the keep-alive back if it should run.
            center.addObserverForName(AVAudioEngineConfigurationChangeNotification, engine, queue) { _ ->
                if (keepAlive) {
                    keepAlive = false
                    setKeepAlive(true)
                }
            },
        )
    }

    /** Calls, Siri, alarms: pause, and pick up again afterwards if iOS says it's appropriate. */
    private fun onInterruption(notification: NSNotification?) {
        val info = notification?.userInfo ?: return
        val type = (info[AVAudioSessionInterruptionTypeKey] as? NSNumber)?.unsignedLongValue ?: return
        if (type == AVAudioSessionInterruptionTypeBegan) {
            val playing = isPlaying()
            resumeAfterInterruption = playing
            if (playing) onCommand(MediaCommand.Pause)
        } else {
            val options = (info[AVAudioSessionInterruptionOptionKey] as? NSNumber)?.unsignedLongValue ?: 0u
            val shouldResume = options and AVAudioSessionInterruptionOptionShouldResume != 0uL
            if (resumeAfterInterruption && shouldResume) {
                audioSession.setActive(true, null)
                onCommand(MediaCommand.Play)
            }
            resumeAfterInterruption = false
        }
    }

    /** Headphones or Bluetooth went away: pause rather than blast through the speaker. */
    private fun onRouteChange(notification: NSNotification?) {
        val reason = (notification?.userInfo?.get(AVAudioSessionRouteChangeReasonKey) as? NSNumber)
            ?.unsignedLongValue
        if (reason == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) onCommand(MediaCommand.Pause)
    }

    private companion object {
        const val KEEP_ALIVE_RATE = 44_100.0
        const val KEEP_ALIVE_FRAMES = 44_100u
    }
}
