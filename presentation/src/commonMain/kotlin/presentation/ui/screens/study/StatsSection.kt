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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.word.model.ProgressStats
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cards_waiting
import lexicon.resources.generated.resources.total_words
import org.jetbrains.compose.resources.stringResource
import theme.Theme

@Composable
fun StatsSection(
    modifier: Modifier = Modifier,
    stats: ProgressStats
) {
    val animatedTotal = rememberAnimatedCounter(target = stats.totalWords)

    // Weighted progress: each level contributes proportionally (Level N = N/6 of full mastery).
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
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = Theme.spacing.small)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular arc progress ring with gradient brush
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(152.dp)) {
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

                    // Gradient mastery progress
                    if (animatedMastery > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(primaryColor, tertiaryColor, primaryColor)
                            ),
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

            // Weighted progress label
            if (stats.totalWords > 0) {
                val progressPercent = (progressFraction * 100).toInt()
                Spacer(Modifier.height(Theme.spacing.extraSmall3))
                Text(
                    text = "Progress $progressPercent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor
                )
            }

            Spacer(Modifier.height(Theme.spacing.extraSmall2))
            when {
                stats.dueCards > 0 -> {
                    Text(
                        text = stringResource(Res.string.cards_waiting, stats.dueCards),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
