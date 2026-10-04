package feature.onboarding.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import components.sheet.SheetPrimaryButton
import components.sheet.SheetTitle
import components.sheet.StepProgressBar
import feature.onboarding.model.OnboardingStep
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.back
import lexicon.resources.generated.resources.skip_preferences
import lexicon.resources.generated.resources.step_of
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/*
 * Shared chrome for the full-screen onboarding flow. It mirrors the bottom-sheet kit
 * (components.sheet): eyebrow + title with an accent highlight, body, footer under a hairline.
 */

/** Caps content at the theme's content max width and adds the standard side gutter. */
@Composable
internal fun Modifier.onboardingContentWidth(): Modifier = this
    .widthIn(max = Theme.dimensions.contentMaxWidth)
    .fillMaxWidth()
    .padding(horizontal = Theme.spacing.md)

/** Back arrow, segmented progress and Skip, shown above every question. */
@Composable
internal fun OnboardingTopBar(
    step: OnboardingStep,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Theme.spacing.xxs)
            .padding(top = Theme.spacing.xs)
            .height(Theme.dimensions.touchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        IconButton(
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
        }
        StepProgressBar(
            current = step.questionNumber,
            total = OnboardingStep.QuestionCount,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = Theme.dimensions.touchTarget)) {
            Text(
                stringResource(Res.string.skip_preferences),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Scrollable page for one question: "Step N of 4", title with accent [highlight], subtitle, then [content]. */
@Composable
internal fun OnboardingQuestionPage(
    step: OnboardingStep,
    title: String,
    highlight: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .onboardingContentWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = Theme.spacing.md, bottom = Theme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.textGap)) {
                Text(
                    stringResource(Res.string.step_of, step.questionNumber, OnboardingStep.QuestionCount),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                SheetTitle(title = title, highlight = highlight)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

/** Sticky action area: optional hairline, then actions stacked with xs spacing above the nav bar. */
@Composable
internal fun OnboardingFooter(
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay))
        }
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .onboardingContentWidth()
                .padding(top = Theme.spacing.sm, bottom = Theme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
            content = content,
        )
    }
}

/** Footer with the single primary action of a question. */
@Composable
internal fun OnboardingPrimaryFooter(text: String, enabled: Boolean, onClick: () -> Unit) {
    OnboardingFooter {
        SheetPrimaryButton(text = text, onClick = onClick, enabled = enabled)
    }
}
