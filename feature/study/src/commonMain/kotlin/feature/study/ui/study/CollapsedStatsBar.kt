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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.word.model.ProgressStats
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.total_words
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

@Composable
fun CollapsedStatsBar(
    visible: Boolean,
    stats: ProgressStats,
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
        Row(
            modifier = Modifier.padding(vertical = Theme.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            MiniProgressRing(stats = stats)

            Text(
                text = "${stats.totalWords} ${stringResource(Res.string.total_words)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (stats.dueCards > 0) {
                Pill(
                    text = "${stats.dueCards} due",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun Pill(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = contentColor,
        modifier = modifier
            .background(
                color = containerColor,
                shape = RoundedCornerShape(Theme.shapes.pill),
            )
            .padding(horizontal = Theme.spacing.xs, vertical = Theme.spacing.xxxs),
    )
}

@Composable
private fun MiniProgressRing(
    stats: ProgressStats,
    modifier: Modifier = Modifier,
) {
    val progressFraction = if (stats.totalWords > 0) {
        val weightedScore = (
                stats.level1Count * 1 +
                        stats.level2Count * 2 +
                        stats.level3Count * 3 +
                        stats.level4Count * 4 +
                        stats.level5Count * 5 +
                        stats.level6Count * 6
                ).toFloat()
        weightedScore / (stats.totalWords * 6f)
    } else 0f

    val progressPercent = (progressFraction * 100).toInt()

    val accentColor = if (stats.totalWords == 0 || progressPercent >= 90) {
        AppColors.master
    } else {
        AppColors.secondary
    }

    ProgressRing(
        progress = progressFraction,
        progressColor = accentColor,
        modifier = Modifier.size(36.dp),
        strokeWidth = 3.5.dp,
        trackColor = MaterialTheme.colorScheme.outlineVariant,
    ) {
        Text(
            text = "$progressPercent",
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
