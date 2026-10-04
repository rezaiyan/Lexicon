package feature.onboarding.model

import utils.Language

/** The screens of the onboarding flow, in order. Every step after [Welcome] is a question. */
enum class OnboardingStep {
    Welcome,
    TargetLanguage,
    NativeLanguage,
    Level,
    DailyGoal;

    /** 1-based position among the questions; 0 for [Welcome]. */
    val questionNumber: Int get() = ordinal

    companion object {
        val QuestionCount: Int = entries.size - 1
    }
}

/** Proficiency the starter words are matched to. [apiValue] is what the backend expects. */
enum class ProficiencyLevel(val apiValue: String) {
    Beginner("beginner"),
    Intermediate("intermediate"),
    Advanced("advanced"),
}

enum class DailyGoalOption(val words: Int) {
    Casual(5),
    Regular(10),
    Serious(20),
    Intense(30),
}

sealed interface OnboardingSubmission {
    data object Idle : OnboardingSubmission
    data object InProgress : OnboardingSubmission
    data class Failed(val message: String) : OnboardingSubmission
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val languages: List<Language> = SupportedLanguages,
    val targetLanguage: Language? = null,
    val nativeLanguage: Language? = null,
    /** The phone's language when it is one we support; offered as the native-language default. */
    val deviceLanguage: Language? = null,
    val level: ProficiencyLevel? = null,
    val dailyGoal: DailyGoalOption = DailyGoalOption.Regular,
    val submission: OnboardingSubmission = OnboardingSubmission.Idle,
) {
    /** Suggested native language, unless it is the language being learned. */
    val nativeSuggestion: Language? get() = deviceLanguage?.takeIf { it != targetLanguage }

    /** Native-language choices below the suggestion: everything except the target and the suggestion. */
    val otherNativeLanguages: List<Language>
        get() = languages.filter { it != targetLanguage && it != nativeSuggestion }

    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.Welcome, OnboardingStep.DailyGoal -> true
            OnboardingStep.TargetLanguage -> targetLanguage != null
            OnboardingStep.NativeLanguage -> nativeLanguage != null
            OnboardingStep.Level -> level != null
        }

    val monthlyWords: Int get() = dailyGoal.words * DaysPerMonth

    companion object {
        private const val DaysPerMonth = 30

        val SupportedLanguages: List<Language> = listOf(
            Language.ENGLISH, Language.GERMAN, Language.FRENCH, Language.SPANISH, Language.ITALIAN,
            Language.PORTUGUESE, Language.DUTCH, Language.RUSSIAN, Language.CHINESE, Language.JAPANESE,
            Language.KOREAN, Language.ARABIC, Language.TURKISH, Language.PERSIAN,
        )
    }
}
