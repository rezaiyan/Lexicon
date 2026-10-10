package domain.tts.model

/**
 * @param speechRate Playback speed multiplier
 * @param expressiveness Piper/VITS noise scale: lower is steadier and clearer, higher varies more like natural speech
 */
data class TtsSettings(
    val speechRate: Float = DEFAULT_SPEECH_RATE,
    val expressiveness: Float = DEFAULT_EXPRESSIVENESS,
) {
    companion object {
        const val DEFAULT_SPEECH_RATE = 1.0f
        const val MIN_SPEECH_RATE = 0.5f
        const val MAX_SPEECH_RATE = 2.0f
        const val DEFAULT_SPEAKER_ID = 0

        /** Piper's training default. */
        const val DEFAULT_EXPRESSIVENESS = 0.667f
        const val MIN_EXPRESSIVENESS = 0.2f
        const val MAX_EXPRESSIVENESS = 1.0f
    }
}
