package utils

enum class Language(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val aiPromptName: String
) {
    ENGLISH("en", "English", "English", "English"),
    PERSIAN("fa", "Persian", "فارسی", "Persian (Farsi)"),
    GERMAN("de", "German", "Deutsch", "German"),
    SPANISH("es", "Spanish", "Español", "Spanish"),
    FRENCH("fr", "French", "Français", "French"),
    ITALIAN("it", "Italian", "Italiano", "Italian"),
    PORTUGUESE("pt", "Portuguese", "Português", "Portuguese"),
    RUSSIAN("ru", "Russian", "Русский", "Russian"),
    CHINESE("zh", "Chinese", "中文", "Chinese (Simplified)"),
    JAPANESE("ja", "Japanese", "日本語", "Japanese"),
    KOREAN("ko", "Korean", "한국어", "Korean"),
    ARABIC("ar", "Arabic", "العربية", "Arabic"),
    TURKISH("tr", "Turkish", "Türkçe", "Turkish"),
    DUTCH("nl", "Dutch", "Nederlands", "Dutch"),
    HINDI("hi", "Hindi", "हिन्दी", "Hindi");

    companion object {
        fun fromCode(code: String): Language {
            return entries.find { it.code == code } ?: ENGLISH
        }
    }
}