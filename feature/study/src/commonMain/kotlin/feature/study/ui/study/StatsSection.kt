package feature.study.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import domain.word.model.ProgressEvaluation
import domain.word.model.ProgressTier
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.all_caught_up
import lexicon.resources.generated.resources.almost_a_master
import lexicon.resources.generated.resources.almost_a_master_subtitle
import lexicon.resources.generated.resources.building_foundation
import lexicon.resources.generated.resources.building_foundation_subtitle
import lexicon.resources.generated.resources.due_count
import lexicon.resources.generated.resources.fully_mastered
import lexicon.resources.generated.resources.fully_mastered_subtitle
import lexicon.resources.generated.resources.getting_started
import lexicon.resources.generated.resources.getting_started_subtitle
import lexicon.resources.generated.resources.import_words
import lexicon.resources.generated.resources.lets_go
import lexicon.resources.generated.resources.making_great_progress
import lexicon.resources.generated.resources.over_halfway
import lexicon.resources.generated.resources.over_halfway_subtitle
import lexicon.resources.generated.resources.progress_subtitle
import lexicon.resources.generated.resources.ready_to_learn
import lexicon.resources.generated.resources.ready_to_learn_subtitle
import lexicon.resources.generated.resources.start_review
import lexicon.resources.generated.resources.strong_knowledge
import lexicon.resources.generated.resources.strong_knowledge_subtitle
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

private val HeroRingSize = 88.dp
private const val MaxDisplayedDue = 99

/**
 * Study-tab hero: overall progress ring, tier headline and a call to action that adapts to
 * the library — import when empty, review (with the due count) when cards are due, and an
 * "all caught up" confirmation otherwise.
 */
@Composable
fun StatsSection(
    evaluation: ProgressEvaluation,
    dueCards: Int,
    onImportWords: () -> Unit,
    onStartReview: () -> Unit,
    onStartReviewLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val accentColor = progressAccent(evaluation.tier)
    val isEmpty = evaluation.tier == ProgressTier.EMPTY

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.large))
            .background(AppColors.primary.copy(alpha = Theme.opacity.hover))
            .padding(Theme.spacing.heroPadding),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.md),
        ) {
            HeroRing(evaluation = evaluation, accentColor = accentColor)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.textGap),
            ) {
                Text(
                    text = tierTitle(evaluation.tier),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = tierSubtitle(evaluation.tier),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when {
            isEmpty -> HeroButton(label = stringResource(Res.string.import_words), onClick = onImportWords)
            dueCards > 0 -> HeroButton(
                label = stringResource(Res.string.start_review),
                badge = stringResource(
                    Res.string.due_count,
                    if (dueCards > MaxDisplayedDue) "$MaxDisplayedDue+" else dueCards.toString(),
                ),
                onClick = onStartReview,
                onLongClick = onStartReviewLongPress,
            )
            else -> CaughtUpRow()
        }
    }
}

/** Accent shared by the hero and the collapsed top-bar ring so both read the same tier color. */
internal fun progressAccent(tier: ProgressTier): Color = when (tier) {
    ProgressTier.EMPTY,
    ProgressTier.ALMOST_MASTER,
    ProgressTier.MASTERED -> AppColors.master

    else -> AppColors.secondary
}

@Composable
private fun HeroRing(evaluation: ProgressEvaluation, accentColor: Color) {
    val isEmpty = evaluation.tier == ProgressTier.EMPTY
    val labelStyle = if (isEmpty) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall
    ProgressRing(
        progress = evaluation.progressFraction,
        progressColor = accentColor,
        trackColor = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier
            .size(HeroRingSize)
            .semantics {
                stateDescription = if (isEmpty) {
                    "No progress yet"
                } else {
                    "Overall progress: ${evaluation.progressPercent}%"
                }
            },
    ) {
        Text(
            text = if (isEmpty) stringResource(Res.string.lets_go) else "${evaluation.progressPercent}%",
            style = labelStyle.copy(lineHeight = 1.1.em),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 10.sp,
                maxFontSize = labelStyle.fontSize,
                stepSize = 1.sp,
            ),
            modifier = Modifier.padding(Theme.spacing.sm),
        )
    }
}

@Composable
private fun HeroButton(
    label: String,
    onClick: () -> Unit,
    badge: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val contentColor = Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Theme.dimensions.touchTarget)
            .clip(RoundedCornerShape(Theme.shapes.pill))
            .background(AppColors.primary)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = Theme.spacing.buttonPaddingHorizontal, vertical = Theme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            maxLines = 1,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 10.sp,
                maxFontSize = MaterialTheme.typography.labelLarge.fontSize,
                stepSize = 1.sp,
            ),
            modifier = Modifier.weight(1f, fill = false),
        )
        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Theme.shapes.pill))
                    .background(contentColor.copy(alpha = Theme.opacity.dragged))
                    .padding(horizontal = Theme.spacing.xs, vertical = Theme.spacing.xxxs),
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun CaughtUpRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Theme.dimensions.touchTarget)
            .clip(RoundedCornerShape(Theme.shapes.pill))
            .background(AppColors.secondary.copy(alpha = Theme.opacity.focus))
            .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = AppColors.secondary,
            modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
        )
        Text(
            text = stringResource(Res.string.all_caught_up),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun tierTitle(tier: ProgressTier): String = when (tier) {
    ProgressTier.EMPTY -> stringResource(Res.string.ready_to_learn)
    ProgressTier.GETTING_STARTED -> stringResource(Res.string.getting_started)
    ProgressTier.BUILDING -> stringResource(Res.string.building_foundation)
    ProgressTier.PROGRESSING -> stringResource(Res.string.making_great_progress)
    ProgressTier.HALFWAY -> stringResource(Res.string.over_halfway)
    ProgressTier.STRONG -> stringResource(Res.string.strong_knowledge)
    ProgressTier.ALMOST_MASTER -> stringResource(Res.string.almost_a_master)
    ProgressTier.MASTERED -> stringResource(Res.string.fully_mastered)
}

@Composable
private fun tierSubtitle(tier: ProgressTier): String = when (tier) {
    ProgressTier.EMPTY -> stringResource(Res.string.ready_to_learn_subtitle)
    ProgressTier.GETTING_STARTED -> stringResource(Res.string.getting_started_subtitle)
    ProgressTier.BUILDING -> stringResource(Res.string.building_foundation_subtitle)
    ProgressTier.PROGRESSING -> stringResource(Res.string.progress_subtitle)
    ProgressTier.HALFWAY -> stringResource(Res.string.over_halfway_subtitle)
    ProgressTier.STRONG -> stringResource(Res.string.strong_knowledge_subtitle)
    ProgressTier.ALMOST_MASTER -> stringResource(Res.string.almost_a_master_subtitle)
    ProgressTier.MASTERED -> stringResource(Res.string.fully_mastered_subtitle)
}
