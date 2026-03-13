package feature.onboarding.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.back
import org.jetbrains.compose.resources.stringResource
import feature.onboarding.model.OnboardingUiState
import feature.onboarding.ui.components.OnboardingIntroContent
import feature.onboarding.ui.components.OnboardingStep1Content
import feature.onboarding.ui.components.OnboardingStep2Content
import feature.onboarding.ui.components.OnboardingStep3Content
import theme.Theme

private const val OnboardingTransitionDuration = 300

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onTargetLanguageSelected: (String) -> Unit,
    onNativeLanguageSelected: (String) -> Unit,
    onLevelSelected: (String) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit
) {
    val spacing = Theme.spacing
    val dimensions = Theme.dimensions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Segmented step progress — only shown for actual steps (not intro)
        if (state.currentStep > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg, vertical = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                if (state.currentStep > 1) {
                    IconButton(
                        onClick = onPreviousStep,
                        enabled = !state.isLoading,
                        modifier = Modifier.size(dimensions.touchTargetSmall)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                            tint = if (state.isLoading)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(dimensions.touchTargetSmall))
                }

                // Animated pill segments
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xxs)
                ) {
                    repeat(state.totalSteps) { index ->
                        val filled = index < state.currentStep
                        val segmentColor by animateColorAsState(
                            targetValue = if (filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            animationSpec = tween(300),
                            label = "segment_$index"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(spacing.xxs)
                                .clip(RoundedCornerShape(spacing.xxxs))
                                .background(segmentColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.size(dimensions.touchTargetSmall))
            }
        }

        AnimatedContent(
            targetState = state.currentStep,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            transitionSpec = {
                val forward = targetState > initialState
                ContentTransform(
                    targetContentEnter = slideInHorizontally(
                        animationSpec = tween(OnboardingTransitionDuration),
                        initialOffsetX = { if (forward) it else -it }
                    ) + fadeIn(animationSpec = tween(OnboardingTransitionDuration)),
                    initialContentExit = slideOutHorizontally(
                        animationSpec = tween(OnboardingTransitionDuration),
                        targetOffsetX = { if (forward) -it else it }
                    ) + fadeOut(animationSpec = tween(OnboardingTransitionDuration))
                )
            },
            label = "onboarding_step"
        ) { step ->
            when (step) {
                0 -> OnboardingIntroContent(
                    onContinue = onNextStep,
                    onSkip = onSkip
                )

                1 -> OnboardingStep1Content(
                    state = state,
                    onTargetLanguageSelected = onTargetLanguageSelected,
                    onNextStep = onNextStep,
                    onSkip = onSkip
                )

                2 -> OnboardingStep2Content(
                    state = state,
                    onNativeLanguageSelected = onNativeLanguageSelected,
                    onNextStep = onNextStep,
                    onSkip = onSkip
                )

                else -> OnboardingStep3Content(
                    state = state,
                    onLevelSelected = onLevelSelected,
                    onSubmit = onSubmit,
                    onBack = onPreviousStep
                )
            }
        }
    }
}
