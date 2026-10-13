package data.tts

/**
 * Splits text for TTS at list separators. Vocabulary often holds alternatives ("he/she",
 * "big, large"): the voice reads "/" aloud and barely pauses at ",", so each part is spoken
 * on its own with a pause between.
 */
object SpeechSegments {

    private val separators = Regex("[,/;|\\\\，、；／]")

    fun split(text: String): List<String> =
        text.split(separators).map { it.trim() }.filter { it.isNotEmpty() }
}
