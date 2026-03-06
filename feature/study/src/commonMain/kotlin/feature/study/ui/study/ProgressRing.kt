package feature.study.ui.study

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable animated progress ring.
 *
 * Size is controlled by the caller via [modifier] (e.g. `Modifier.size(110.dp)`).
 *
 * @param progress Target fraction in 0f..1f — animated internally.
 * @param progressColor Solid color for the progress arc.
 */
@Composable
fun ProgressRing(
    progress: Float,
    progressColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    trackColor: Color = Color.Unspecified,
    startAngle: Float = -90f,
    sweepAngle: Float = 360f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    ProgressRing(
        progress = progress,
        progressBrush = SolidColor(progressColor),
        modifier = modifier,
        strokeWidth = strokeWidth,
        trackColor = trackColor,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        content = content
    )
}

/**
 * Reusable animated progress ring (brush variant for gradient arcs).
 *
 * @param progress Target fraction in 0f..1f — animated internally.
 * @param progressBrush Brush used to paint the progress arc (gradient, solid, etc.).
 */
@Composable
fun ProgressRing(
    progress: Float,
    progressBrush: Brush,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    trackColor: Color = Color.Unspecified,
    startAngle: Float = -90f,
    sweepAngle: Float = 360f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progressRing"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val sw = strokeWidth.toPx()
            val diameter = size.minDimension - sw
            val topLeft = Offset(
                x = (size.width - diameter) / 2,
                y = (size.height - diameter) / 2
            )
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(sw, cap = StrokeCap.Round)
            )

            if (animatedProgress > 0f) {
                drawArc(
                    brush = progressBrush,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(sw, cap = StrokeCap.Round)
                )
            }
        }

        content()
    }
}
