package components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Gradient variant of the animated progress bar (e.g. for review progress strips).
 *
 * @param progress Target progress fraction (0f..1f).
 * @param gradientColors Colors for the horizontal gradient fill.
 * @param modifier Optional modifier.
 * @param trackColor Background track color.
 * @param height Bar height. Defaults to 3.dp.
 * @param animationDurationMs Duration for the progress animation.
 */
@Composable
fun GradientProgressBar(
    progress: Float,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    trackColor: Color = Color.Unspecified,
    height: Dp = 3.dp,
    animationDurationMs: Int = 350
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = animationDurationMs, easing = FastOutSlowInEasing),
        label = "gradientProgress"
    )

    Box(modifier = modifier.fillMaxWidth().height(height)) {
        if (trackColor != Color.Unspecified) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .background(trackColor)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(height)
                .background(Brush.horizontalGradient(gradientColors))
        )
    }
}
