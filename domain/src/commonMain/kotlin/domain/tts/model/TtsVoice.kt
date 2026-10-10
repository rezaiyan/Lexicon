package domain.tts.model

/**
 * One downloadable voice for a language.
 *
 * @param id Stable key persisted as the user's choice (e.g. "en_US-kristin-medium")
 * @param name Speaker name shown to the user (e.g. "Kristin")
 * @param region Accent region code (e.g. "US", "GB"), null when the voice has no regional variant
 */
data class TtsVoice(
    val id: String,
    val name: String,
    val region: String? = null,
)
