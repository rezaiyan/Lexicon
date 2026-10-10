package feature.study.ui.study

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.word.model.ProgressEvaluation
import domain.word.model.ProgressStats
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.due_count
import lexicon.resources.generated.resources.words_label
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

@Composable
fun CollapsedStatsBar(
    visible: Boolean,
    stats: ProgressStats,
    evaluation: ProgressEvaluation,
    modifier: Modifier = Modifier,
) {
    val enterTransition = slideInVertically(
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        initialOffsetY = { -it }
    ) + fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))

    val exitTransition = slideOutVertically(
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        targetOffsetY = { -it }
    ) + fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))

    AnimatedVisibility(
        visible = visible,
        enter = enterTransition,
        exit = exitTransition,
        modifier = modifier,
    ) {
        BoxWithConstraints {
            // Narrow title slot (small phones, large font scale): drop the "words" label first,
            // the due pill is the actionable bit and must never be squeezed.
            val isCompact = maxWidth < CompactWidth
            Row(
                modifier = Modifier
                    .padding(vertical = Theme.spacing.xxs)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
            ) {
                MiniProgressRing(evaluation = evaluation)

                // Weighted (fill = false) so it's measured after the pill and ellipsizes instead
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(stats.totalWords.toString())
                        }
                        if (!isCompact) {
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            ) {
                                append(" ")
                                append(stringResource(Res.string.words_label))
                            }
                        }
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                if (stats.dueCards > 0) {
                    DuePill(count = stats.dueCards)
                }
            }
        }
    }
}

@Composable
private fun DuePill(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val accent = AppColors.primary
    val label = if (count > MaxDisplayedDue) "$MaxDisplayedDue+" else count.toString()
    Row(
        modifier = modifier
            .heightIn(min = Theme.spacing.lg)
            .clip(RoundedCornerShape(Theme.shapes.pill))
            .background(accent.copy(alpha = Theme.opacity.focus))
            .padding(horizontal = Theme.spacing.xs, vertical = Theme.spacing.xxxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs + Theme.spacing.xxxs),
    ) {
        Box(
            modifier = Modifier
                .size(Theme.spacing.xxs + Theme.spacing.xxxs)
                .background(accent, CircleShape),
        )
        Text(
            text = stringResource(Res.string.due_count, label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            maxLines = 1,
            softWrap = false,
        )
    }
}

private val CompactWidth = 200.dp
private const val MaxDisplayedDue = 99

@Composable
private fun MiniProgressRing(
    evaluation: ProgressEvaluation,
) {
    ProgressRing(
        progress = evaluation.progressFraction,
        progressColor = progressAccent(evaluation.tier),
        modifier = Modifier.size(36.dp)
            .semantics {
                stateDescription = "Progress: ${evaluation.progressPercent}%"
            },
        strokeWidth = 3.5.dp,
        trackColor = MaterialTheme.colorScheme.outlineVariant,
    ) {
        Text(
            text = "${evaluation.progressPercent}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                lineHeight = 9.sp,
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}
