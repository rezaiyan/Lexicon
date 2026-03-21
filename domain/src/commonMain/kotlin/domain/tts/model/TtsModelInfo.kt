package domain.tts.model

/**
 * Represents information about a TTS model for a specific language.
 *
 * @param languageCode ISO 639-1 language code (e.g., "en", "de")
 * @param languageDisplayName Human-readable language name (e.g., "English")
 * @param isDownloaded Whether the model files are present on disk
 * @param sizeBytes Size of the model files in bytes (0 if not downloaded)
 */
data class TtsModelInfo(
    val languageCode: String,
    val languageDisplayName: String,
    val isDownloaded: Boolean,
    val sizeBytes: Long,
    val numSpeakers: Int = 1,
    val selectedSpeakerId: Int = 0,
)
