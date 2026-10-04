package feature.onboarding.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import domain.onboarding.model.ProficiencyLevel
import expects.BackHandler
import feature.onboarding.model.DailyGoalOption
import feature.onboarding.model.OnboardingStep
import feature.onboarding.model.OnboardingSubmission
import feature.onboarding.model.OnboardingUiState
import feature.onboarding.ui.components.DailyGoalQuestion
import feature.onboarding.ui.components.LevelQuestion
import feature.onboarding.ui.components.NativeLanguageQuestion
import feature.onboarding.ui.components.OnboardingBuildFailed
import feature.onboarding.ui.components.OnboardingBuilding
import feature.onboarding.ui.components.OnboardingPrimaryFooter
import feature.onboarding.ui.components.OnboardingTopBar
import feature.onboarding.ui.components.OnboardingWelcome
import feature.onboarding.ui.components.TargetLanguageQuestion
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.advanced
import lexicon.resources.generated.resources.beginner
import lexicon.resources.generated.resources.intermediate
import lexicon.resources.generated.resources.onboarding_build_word_list
import lexicon.resources.generated.resources.onboarding_continue
import lexicon.resources.generated.resources.onboarding_words_a_day_short
import org.jetbrains.compose.resources.stringResource
import utils.Language

private const val StepTransitionMillis = 300

/** Which of the three onboarding layouts is on screen. */
private enum class OnboardingLayout { Welcome, Questions, Building }

private val OnboardingUiState.layout: OnboardingLayout
    get() = when {
        submission != OnboardingSubmission.Idle -> OnboardingLayout.Building
        step == OnboardingStep.Welcome -> OnboardingLayout.Welcome
        else -> OnboardingLayout.Questions
    }

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onTargetLanguageSelected: (Language) -> Unit,
    onNativeLanguageSelected: (Language) -> Unit,
    onLevelSelected: (ProficiencyLevel) -> Unit,
    onDailyGoalSelected: (DailyGoalOption) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
) {
    BackHandler(enabled = state.layout != OnboardingLayout.Welcome, onBack = onBack)

    AnimatedContent(
        targetState = state.layout,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        transitionSpec = { fadeIn(tween(StepTransitionMillis)) togetherWith fadeOut(tween(StepTransitionMillis)) },
        label = "onboarding_layout",
    ) { layout ->
        when (layout) {
            OnboardingLayout.Welcome -> OnboardingWelcome(onGetStarted = onNext, onSkip = onSkip)
            OnboardingLayout.Questions -> OnboardingQuestions(
                state = state,
                onTargetLanguageSelected = onTargetLanguageSelected,
                onNativeLanguageSelected = onNativeLanguageSelected,
                onLevelSelected = onLevelSelected,
                onDailyGoalSelected = onDailyGoalSelected,
                onNext = onNext,
                onBack = onBack,
                onSkip = onSkip,
            )
            OnboardingLayout.Building -> when (val submission = state.submission) {
                is OnboardingSubmission.Failed -> OnboardingBuildFailed(
                    message = submission.message,
                    onRetry = onRetry,
                    onStartEmpty = onSkip,
                )
                else -> OnboardingBuilding(summary = answersSummary(state))
            }
        }
    }
}

@Composable
private fun OnboardingQuestions(
    state: OnboardingUiState,
    onTargetLanguageSelected: (Language) -> Unit,
    onNativeLanguageSelected: (Language) -> Unit,
    onLevelSelected: (ProficiencyLevel) -> Unit,
    onDailyGoalSelected: (DailyGoalOption) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OnboardingTopBar(step = state.step, onBack = onBack, onSkip = onSkip)
        AnimatedContent(
            targetState = state.step,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                val slide = tween<IntOffset>(StepTransitionMillis)
                val fade = tween<Float>(StepTransitionMillis)
                ContentTransform(
                    targetContentEnter = slideInHorizontally(slide) { it * direction } + fadeIn(fade),
                    initialContentExit = slideOutHorizontally(slide) { -it * direction } + fadeOut(fade),
                )
            },
            label = "onboarding_step",
        ) { step ->
            when (step) {
                OnboardingStep.TargetLanguage -> TargetLanguageQuestion(
                    languages = state.languages,
                    selected = state.targetLanguage,
                    onSelected = onTargetLanguageSelected,
                )
                OnboardingStep.NativeLanguage -> NativeLanguageQuestion(
                    suggestion = state.nativeSuggestion,
                    others = state.otherNativeLanguages,
                    selected = state.nativeLanguage,
                    onSelected = onNativeLanguageSelected,
                )
                OnboardingStep.Level -> LevelQuestion(selected = state.level, onSelected = onLevelSelected)
                OnboardingStep.DailyGoal -> DailyGoalQuestion(
                    selected = state.dailyGoal,
                    monthlyWords = state.monthlyWords,
                    onSelected = onDailyGoalSelected,
                )
                // Welcome has its own layout and never reaches this content
                OnboardingStep.Welcome -> Unit
            }
        }
        OnboardingPrimaryFooter(
            text = stringResource(
                if (state.step == OnboardingStep.DailyGoal) Res.string.onboarding_build_word_list
                else Res.string.onboarding_continue
            ),
            enabled = state.canContinue,
            onClick = onNext,
        )
    }
}

/** "German · Intermediate · 10 a day" */
@Composable
private fun answersSummary(state: OnboardingUiState): String {
    val level = when (state.level) {
        ProficiencyLevel.BEGINNER -> stringResource(Res.string.beginner)
        ProficiencyLevel.INTERMEDIATE -> stringResource(Res.string.intermediate)
        ProficiencyLevel.ADVANCED -> stringResource(Res.string.advanced)
        null -> null
    }
    return listOfNotNull(
        state.targetLanguage?.displayName,
        level,
        stringResource(Res.string.onboarding_words_a_day_short, state.dailyGoal.words),
    ).joinToString(" · ")
}
