package domain.settings.model

enum class ThemeMode(val displayName: String) {
    AUTO("Automatic"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromString(value: String): ThemeMode {
            return entries.find { it.name == value } ?: AUTO
        }
    }
}
