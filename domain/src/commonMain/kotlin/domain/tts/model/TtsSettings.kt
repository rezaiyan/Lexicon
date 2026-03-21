package domain.tts.model

data class TtsSettings(
    val speechRate: Float = DEFAULT_SPEECH_RATE,
) {
    companion object {
        const val DEFAULT_SPEECH_RATE = 1.0f
        const val MIN_SPEECH_RATE = 0.5f
        const val MAX_SPEECH_RATE = 2.0f
        const val DEFAULT_SPEAKER_ID = 0
    }
}
