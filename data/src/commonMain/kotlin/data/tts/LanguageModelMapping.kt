package data.tts

import domain.tts.model.TtsVoice

/**
 * Maps language codes to sherpa-onnx compatible Piper TTS model archives.
 *
 * Each model is a `.tar.bz2` archive (~64MB) from the sherpa-onnx releases
 * containing the `.onnx` model, `tokens.txt`, and `espeak-ng-data/` directory.
 * A language offers several voices; the first is the default and must stay first,
 * since installs made before voice choice existed have that voice on disk.
 */
object LanguageModelMapping {

    data class PiperModelInfo(
        val voiceId: String,
        val name: String,
        val region: String?,
        val archiveUrl: String,
        val extractedDirName: String,
        val numSpeakers: Int = 1,
    )

    private data class LanguageVoices(
        val sampleText: String,
        val voices: List<PiperModelInfo>,
    )

    private const val BASE = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"

    /** [id] is the archive name without the `vits-piper-` prefix and `.tar.bz2` suffix. */
    private fun voice(id: String, name: String, region: String? = null, numSpeakers: Int = 1) =
        PiperModelInfo(
            voiceId = id,
            name = name,
            region = region,
            archiveUrl = "$BASE/vits-piper-$id.tar.bz2",
            extractedDirName = "vits-piper-$id",
            numSpeakers = numSpeakers,
        )

    private val languageModels = mapOf(
        "en" to LanguageVoices(
            sampleText = "Hello! This is how I will read your words.",
            voices = listOf(
                voice("en_US-kristin-medium", "Kristin", "US"),
                voice("en_US-lessac-medium", "Lessac", "US"),
                voice("en_US-amy-medium", "Amy", "US"),
                voice("en_US-ryan-medium", "Ryan", "US"),
                voice("en_US-joe-medium", "Joe", "US"),
                voice("en_GB-cori-medium", "Cori", "GB"),
                voice("en_GB-alan-medium", "Alan", "GB"),
            ),
        ),
        "de" to LanguageVoices(
            sampleText = "Hallo! So lese ich deine Wörter vor.",
            voices = listOf(
                voice("de_DE-thorsten-medium", "Thorsten"),
                voice("de_DE-kerstin-low", "Kerstin"),
                voice("de_DE-ramona-low", "Ramona"),
            ),
        ),
        "es" to LanguageVoices(
            sampleText = "¡Hola! Así leeré tus palabras.",
            voices = listOf(
                voice("es_MX-ald-medium", "Ald", "MX"),
                voice("es_ES-davefx-medium", "Davefx", "ES"),
                voice("es_AR-daniela-high", "Daniela", "AR"),
            ),
        ),
        "fr" to LanguageVoices(
            sampleText = "Bonjour ! Voici comment je lirai tes mots.",
            voices = listOf(
                voice("fr_FR-siwis-medium", "Siwis"),
                voice("fr_FR-tom-medium", "Tom"),
                voice("fr_FR-upmc-medium", "UPMC", numSpeakers = 2),
            ),
        ),
        "it" to LanguageVoices(
            sampleText = "Ciao! Ecco come leggerò le tue parole.",
            voices = listOf(
                voice("it_IT-riccardo-x_low", "Riccardo"),
                voice("it_IT-paola-medium", "Paola"),
            ),
        ),
        "pt" to LanguageVoices(
            sampleText = "Olá! É assim que vou ler as suas palavras.",
            voices = listOf(
                voice("pt_BR-faber-medium", "Faber", "BR"),
                voice("pt_BR-cadu-medium", "Cadu", "BR"),
                voice("pt_PT-tugao-medium", "Tugão", "PT"),
            ),
        ),
        "ru" to LanguageVoices(
            sampleText = "Привет! Вот так я буду читать твои слова.",
            voices = listOf(
                voice("ru_RU-ruslan-medium", "Ruslan"),
                voice("ru_RU-irina-medium", "Irina"),
                voice("ru_RU-denis-medium", "Denis"),
            ),
        ),
        "zh" to LanguageVoices(
            sampleText = "你好！我会这样读你的单词。",
            voices = listOf(
                voice("zh_CN-huayan-medium", "Huayan"),
                voice("zh_CN-xiao_ya-medium", "Xiao Ya"),
                voice("zh_CN-chaowen-medium", "Chaowen"),
            ),
        ),
        "tr" to LanguageVoices(
            sampleText = "Merhaba! Kelimelerini böyle okuyacağım.",
            voices = listOf(
                voice("tr_TR-fettah-medium", "Fettah"),
                voice("tr_TR-dfki-medium", "DFKI"),
                voice("tr_TR-fahrettin-medium", "Fahrettin"),
            ),
        ),
        "nl" to LanguageVoices(
            sampleText = "Hallo! Zo lees ik je woorden voor.",
            voices = listOf(
                voice("nl_NL-miro-high", "Miro", "NL"),
                voice("nl_NL-pim-medium", "Pim", "NL"),
                voice("nl_BE-nathalie-medium", "Nathalie", "BE"),
            ),
        ),
        "ar" to LanguageVoices(
            sampleText = "مرحبا! هكذا سأقرأ كلماتك.",
            voices = listOf(
                voice("ar_JO-kareem-medium", "Kareem"),
            ),
        ),
        "hi" to LanguageVoices(
            sampleText = "नमस्ते! मैं आपके शब्द ऐसे पढ़ूँगा।",
            voices = listOf(
                voice("hi_IN-rohan-medium", "Rohan"),
                voice("hi_IN-pratham-medium", "Pratham"),
                voice("hi_IN-priyamvada-medium", "Priyamvada"),
            ),
        ),
        "fa" to LanguageVoices(
            sampleText = "سلام! من کلمات شما را این‌گونه می‌خوانم.",
            voices = listOf(
                voice("fa-haaniye_low", "Haaniye"),
                voice("fa_IR-amir-medium", "Amir"),
                voice("fa_IR-ganji-medium", "Ganji"),
                voice("fa_IR-reza_ibrahim-medium", "Reza"),
            ),
        ),
    )

    /** Model for [voiceId], falling back to the language's default voice when null or unknown. */
    fun getModelInfo(languageCode: String, voiceId: String? = null): PiperModelInfo? {
        val voices = languageModels[languageCode]?.voices ?: return null
        return voices.firstOrNull { it.voiceId == voiceId } ?: voices.first()
    }

    fun getVoices(languageCode: String): List<TtsVoice> =
        languageModels[languageCode]?.voices.orEmpty().map { TtsVoice(it.voiceId, it.name, it.region) }

    fun getSampleText(languageCode: String): String = languageModels[languageCode]?.sampleText.orEmpty()

    fun isSupported(languageCode: String): Boolean = languageModels.containsKey(languageCode)

    val supportedLanguages: Set<String> = languageModels.keys
}
