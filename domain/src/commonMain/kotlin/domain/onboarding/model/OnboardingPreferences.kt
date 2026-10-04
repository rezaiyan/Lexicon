package domain.onboarding.model

import utils.Language

/** Answers used to generate suggested vocabulary (onboarding and the AI word wizard). */
data class OnboardingPreferences(
    /** Language being learned. */
    val targetLanguage: Language,
    /** Language the translations are written in. */
    val nativeLanguage: Language,
    val level: ProficiencyLevel,
    val interests: List<String> = emptyList(),
) {
    companion object {
        /** Languages the suggestion service generates words for. */
        val SupportedLanguages: List<Language> = listOf(
            Language.ENGLISH, Language.GERMAN, Language.FRENCH, Language.SPANISH, Language.ITALIAN,
            Language.PORTUGUESE, Language.DUTCH, Language.RUSSIAN, Language.CHINESE, Language.JAPANESE,
            Language.KOREAN, Language.ARABIC, Language.TURKISH, Language.PERSIAN,
        )
    }
}
