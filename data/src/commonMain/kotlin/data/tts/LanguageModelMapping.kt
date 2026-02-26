package data.tts

/**
 * Maps language codes to sherpa-onnx compatible Piper TTS model archives.
 *
 * Each model is a `.tar.bz2` archive (~64MB) from the sherpa-onnx releases
 * containing the `.onnx` model, `tokens.txt`, and `espeak-ng-data/` directory.
 */
object LanguageModelMapping {

    data class PiperModelInfo(
        val archiveUrl: String,
        val extractedDirName: String
    )

    private const val BASE = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"

    private val languageModels = mapOf(
        "en" to PiperModelInfo(
            archiveUrl = "$BASE/vits-piper-en_US-kristin-medium.tar.bz2",
            extractedDirName = "vits-piper-en_US-kristin-medium"
        ),
        "de" to PiperModelInfo(
            archiveUrl = "$BASE/vits-piper-de_DE-thorsten-medium.tar.bz2",
            extractedDirName = "vits-piper-de_DE-thorsten-medium"
        ),
        "es" to PiperModelInfo(
            archiveUrl = "$BASE/vits-piper-es_MX-ald-medium.tar.bz2",
            extractedDirName = "vits-piper-es_MX-ald-medium"
        ),
        "fr" to PiperModelInfo(
            archiveUrl = "$BASE/vits-piper-fr_FR-siwis-medium.tar.bz2",
            extractedDirName = "vits-piper-fr_FR-siwis-medium"
        ),
        "it" to PiperModelInfo(
            archiveUrl = "$BASE/vits-piper-it_IT-riccardo-x_low.tar.bz2",
            extractedDirName = "vits-piper-it_IT-riccardo-x_low"
        ),
        "pt" to PiperModelInfo(
            archiveUrl = "$BASE/vits-piper-pt_BR-faber-medium.tar.bz2",
            extractedDirName = "vits-piper-pt_BR-faber-medium"
        ),
        "ru" to PiperModelInfo(
            archiveUrl = "$BASE/vits-piper-ru_RU-ruslan-medium.tar.bz2",
            extractedDirName = "vits-piper-ru_RU-ruslan-medium"
        ),
        "zh" to PiperModelInfo(
            archiveUrl = "$BASE/vits-piper-zh_CN-huayan-medium.tar.bz2",
            extractedDirName = "vits-piper-zh_CN-huayan-medium"
        ),
        "tr" to PiperModelInfo(
            archiveUrl = "$BASE/vits-piper-tr_TR-fettah-medium.tar.bz2",
            extractedDirName = "vits-piper-tr_TR-fettah-medium"
        ),
        "nl" to PiperModelInfo(
            archiveUrl = "$BASE/vits-piper-nl_NL-miro-high.tar.bz2",
            extractedDirName = "vits-piper-nl_NL-miro-high"
        ),
        "ar" to PiperModelInfo(
            archiveUrl = "$BASE/vits-piper-ar_JO-kareem-medium.tar.bz2",
            extractedDirName = "vits-piper-ar_JO-kareem-medium"
        ),
        "hi" to PiperModelInfo(
            archiveUrl = "$BASE/vits-piper-hi_IN-rohan-medium.tar.bz2",
            extractedDirName = "vits-piper-hi_IN-rohan-medium"
        ),
        "fa" to PiperModelInfo(
            archiveUrl = "$BASE/vits-piper-fa-haaniye_low.tar.bz2",
            extractedDirName = "vits-piper-fa-haaniye_low"
        ),
    )

    fun getModelInfo(languageCode: String): PiperModelInfo? = languageModels[languageCode]

    fun isSupported(languageCode: String): Boolean = languageModels.containsKey(languageCode)

    val supportedLanguages: Set<String> = languageModels.keys
}
