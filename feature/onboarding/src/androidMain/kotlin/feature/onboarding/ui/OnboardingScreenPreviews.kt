package feature.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import domain.onboarding.model.SuggestedVocabulary
import feature.onboarding.model.DailyGoalOption
import feature.onboarding.model.OnboardingStep
import feature.onboarding.model.OnboardingSubmission
import feature.onboarding.model.OnboardingUiState
import feature.onboarding.model.ProficiencyLevel
import feature.onboarding.model.VocabularyPreviewUiState
import theme.LexiconTheme
import utils.Language

private val Answered = OnboardingUiState(
    targetLanguage = Language.GERMAN,
    nativeLanguage = Language.ENGLISH,
    deviceLanguage = Language.ENGLISH,
    level = ProficiencyLevel.Intermediate,
    dailyGoal = DailyGoalOption.Regular,
)

@Composable
private fun Onboarding(state: OnboardingUiState) {
    LexiconTheme {
        OnboardingScreen(
            state = state,
            onTargetLanguageSelected = {},
            onNativeLanguageSelected = {},
            onLevelSelected = {},
            onDailyGoalSelected = {},
            onNext = {},
            onBack = {},
            onRetry = {},
            onSkip = {},
        )
    }
}

@Preview(name = "Welcome", showSystemUi = true)
@Composable
private fun WelcomePreview() = Onboarding(OnboardingUiState())

@Preview(name = "1 Language to learn", showSystemUi = true)
@Composable
private fun TargetLanguagePreview() = Onboarding(Answered.copy(step = OnboardingStep.TargetLanguage))

@Preview(name = "2 Native language", showSystemUi = true)
@Composable
private fun NativeLanguagePreview() = Onboarding(Answered.copy(step = OnboardingStep.NativeLanguage))

@Preview(name = "3 Level", showSystemUi = true)
@Composable
private fun LevelPreview() = Onboarding(Answered.copy(step = OnboardingStep.Level))

@Preview(name = "4 Daily goal", showSystemUi = true)
@Composable
private fun DailyGoalPreview() = Onboarding(Answered.copy(step = OnboardingStep.DailyGoal))

@Preview(name = "Building starter words", showSystemUi = true)
@Composable
private fun BuildingPreview() = Onboarding(
    Answered.copy(step = OnboardingStep.DailyGoal, submission = OnboardingSubmission.InProgress)
)

@Preview(name = "Building failed", showSystemUi = true)
@Composable
private fun BuildFailedPreview() = Onboarding(
    Answered.copy(
        step = OnboardingStep.DailyGoal,
        submission = OnboardingSubmission.Failed("No internet connection. Check your network and try again."),
    )
)

@Preview(name = "Starter words", showSystemUi = true)
@Composable
private fun StarterWordsPreview() {
    LexiconTheme {
        VocabularyPreviewScreen(
            state = VocabularyPreviewUiState(
                words = listOf(
                    SuggestedVocabulary("die Erfahrung", "experience", "", "English", "German"),
                    SuggestedVocabulary("sich bewerben", "to apply", "", "English", "German"),
                    SuggestedVocabulary("zuverlässig", "reliable", "", "English", "German"),
                ),
                learningLanguage = Language.GERMAN,
                nativeLanguage = Language.ENGLISH,
            ),
            onAddWords = {},
            onStartEmpty = {},
        )
    }
}
