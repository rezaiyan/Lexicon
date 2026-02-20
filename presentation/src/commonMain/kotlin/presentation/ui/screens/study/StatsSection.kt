package presentation.ui.screens.study

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.word.model.ProgressStats
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cards_due_for_review
import lexicon.resources.generated.resources.total_words

@Composable
fun StatsSection(
    modifier: Modifier = Modifier,
    stats: ProgressStats,
    onStartReview: () -> Unit
) {
    val animatedTotal = rememberAnimatedCounter(target = stats.totalWords)

    // Weighted progress: each level contributes proportionally (Level N = N/6 of full mastery).
    // This means every review session moves the needle — even early-stage words show progress.
    // Formula: Σ(count_at_levelN × N) / (totalWords × 6)
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

    val animatedMastery by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "mastery"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = Theme.spacing.sectionSpacing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular arc progress ring with animated word count in the centre
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(168.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(
                        x = (size.width - diameter) / 2,
                        y = (size.height - diameter) / 2
                    )
                    val arcSize = Size(diameter, diameter)

                    // Background track
                    drawArc(
                        color = trackColor,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )

                    // Mastery progress
                    if (animatedMastery > 0f) {
                        drawArc(
                            color = primaryColor,
                            startAngle = 135f,
                            sweepAngle = 270f * animatedMastery,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = animatedTotal.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(Res.string.total_words),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Weighted progress label — visible from the very first study session
            if (stats.totalWords > 0) {
                val progressPercent = (progressFraction * 100).toInt()
                Spacer(Modifier.height(Theme.spacing.extraSmall3))
                Text(
                    text = "Progress $progressPercent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor
                )
            }

            // Due cards info
            if (stats.dueCards > 0) {
                Spacer(Modifier.height(Theme.spacing.extraSmall2))
                Text(
                    text = stringResource(Res.string.cards_due_for_review, stats.dueCards),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ReviewActionSection(
                hasDueCards = stats.dueCards > 0,
                onStartReview = onStartReview
            )
        }
    }
}
