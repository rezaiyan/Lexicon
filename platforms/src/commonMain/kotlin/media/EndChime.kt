package media

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * The soft two-note chime played when a listening session finishes, so someone with the phone in
 * their pocket knows it's done. Synthesized (no audio asset): a rising fifth with a gentle decay.
 */
object EndChime {
    const val SAMPLE_RATE = 44_100
    private const val NOTE_SECONDS = 0.22
    private const val TAIL_SECONDS = 0.30
    private const val FIRST_HZ = 659.25 // E5
    private const val SECOND_HZ = 987.77 // B5
    private const val PEAK = 0.22
    private const val DECAY = 9.0
    private const val ATTACK_SECONDS = 0.008

    /** Mono float PCM in [-1, 1] at [SAMPLE_RATE]. */
    fun samples(): FloatArray {
        val noteFrames = (SAMPLE_RATE * NOTE_SECONDS).toInt()
        val total = noteFrames + (SAMPLE_RATE * (NOTE_SECONDS + TAIL_SECONDS)).toInt()
        return FloatArray(total) { frame ->
            val first = tone(frame, FIRST_HZ)
            val second = if (frame >= noteFrames) tone(frame - noteFrames, SECOND_HZ) else 0.0
            (first + second).coerceIn(-1.0, 1.0).toFloat()
        }
    }

    private fun tone(frame: Int, hz: Double): Double {
        val t = frame.toDouble() / SAMPLE_RATE
        val attack = (t / ATTACK_SECONDS).coerceAtMost(1.0)
        return PEAK * attack * exp(-DECAY * t) * sin(2 * PI * hz * t)
    }
}
