package presentation.ui.screens.study

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import domain.word.model.ProgressStats
import theme.AppColors
import theme.Theme

private data class LevelSegment(
    val count: Int,
    val color: Color
)

@Composable
fun WordDistributionBar(
    stats: ProgressStats,
    modifier: Modifier = Modifier
) {
    if (stats.totalWords == 0) return

    val segments = listOf(
        LevelSegment(stats.level0Count, AppColors.novice),
        LevelSegment(stats.level1Count, AppColors.apprentice),
        LevelSegment(stats.level2Count, AppColors.apprentice),
        LevelSegment(stats.level3Count, AppColors.adept),
        LevelSegment(stats.level4Count, AppColors.adept),
        LevelSegment(stats.level5Count, AppColors.master),
        LevelSegment(stats.level6Count, AppColors.master)
    )

    // Animate the overall bar entrance
    val barProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "distributionBar"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .padding(bottom = Theme.spacing.small)
            .clip(RoundedCornerShape(4.dp))
    ) {
        segments.forEach { segment ->
            val fraction = if (segment.count > 0) {
                segment.count.toFloat() / stats.totalWords
            } else {
                0f
            }

            if (segment.count > 0) {
                // Proportional filled segment
                Box(
                    modifier = Modifier
                        .weight(fraction * barProgress)
                        .fillMaxHeight()
                        .background(segment.color.copy(alpha = 0.8f))
                )
            } else {
                // Thin minimum-width placeholder for empty levels
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(segment.color.copy(alpha = 0.15f))
                )
            }
        }
    }
}
