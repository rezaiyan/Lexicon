package feature.onboarding.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import components.GeneratingProgress
import components.sheet.SheetPrimaryButton
import components.sheet.SheetTextButton
import components.sheet.StepProgressBar
import feature.onboarding.model.OnboardingStep
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.onboarding_building_title
import lexicon.resources.generated.resources.onboarding_failed_title
import lexicon.resources.generated.resources.onboarding_start_empty
import lexicon.resources.generated.resources.onboarding_step_choosing
import lexicon.resources.generated.resources.onboarding_step_packing
import lexicon.resources.generated.resources.onboarding_step_translating
import lexicon.resources.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private val BuildingSteps = listOf(
    Res.string.onboarding_step_choosing,
    Res.string.onboarding_step_translating,
    Res.string.onboarding_step_packing,
)

/**
 * Shown after the last question while starter words are generated.
 * [summary] recaps the answers ("German · Intermediate · 10 a day").
 */
@Composable
internal fun OnboardingBuilding(summary: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        CompletedProgress()
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            GeneratingProgress(
                title = stringResource(Res.string.onboarding_building_title),
                steps = BuildingSteps.map { stringResource(it) },
                summary = summary,
                modifier = Modifier
                    .onboardingContentWidth()
                    .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.xxxl),
            )
        }
    }
}

/** Generation failed: say why, offer a retry or an empty start. System back returns to the questions. */
@Composable
internal fun OnboardingBuildFailed(
    message: String,
    onRetry: () -> Unit,
    onStartEmpty: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        CompletedProgress()
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.onboardingContentWidth().padding(horizontal = Theme.spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
            ) {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(Theme.dimensions.iconSizeMassive),
                )
                Text(
                    stringResource(Res.string.onboarding_failed_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        OnboardingFooter(showDivider = false) {
            SheetPrimaryButton(text = stringResource(Res.string.retry), onClick = onRetry)
            SheetTextButton(text = stringResource(Res.string.onboarding_start_empty), onClick = onStartEmpty)
        }
    }
}

/** All four segments filled: the questions are done. Same position as the question top bar. */
@Composable
private fun CompletedProgress() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = Theme.spacing.xs)
            .height(Theme.dimensions.touchTarget)
            // Inset like the question top bar's back button so the bar doesn't jump between screens
            .padding(horizontal = Theme.spacing.xxs + Theme.dimensions.touchTarget + Theme.spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        StepProgressBar(
            current = OnboardingStep.QuestionCount,
            total = OnboardingStep.QuestionCount,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
