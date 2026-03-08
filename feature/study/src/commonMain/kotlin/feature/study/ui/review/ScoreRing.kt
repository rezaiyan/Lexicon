package feature.study.ui.review

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import components.animation.rememberAnimatedCounter
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.completion_score_percent
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ScoreRing(
    scorePercent: Int,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val sweepTarget = scorePercent / 100f
    val animatedSweep = remember { Animatable(0f) }
    val animatedCount = rememberAnimatedCounter(scorePercent, durationMillis = 1200)

    LaunchedEffect(sweepTarget) {
        animatedSweep.animateTo(
            targetValue = sweepTarget,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        )
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val gradientBrush = Brush.sweepGradient(gradientColors)

    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val padding = strokeWidth / 2

            val arcTopLeft = Offset(padding, padding)
            val arcSize = Size(
                size.width - strokeWidth,
                size.height - strokeWidth,
            )

            // Track (soft, semi-transparent)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = arcTopLeft,
                size = arcSize,
            )

            // Gradient progress arc
            drawArc(
                brush = gradientBrush,
                startAngle = -90f,
                sweepAngle = 360f * animatedSweep.value,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = arcTopLeft,
                size = arcSize,
            )
        }

        // Score percentage text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.completion_score_percent, animatedCount),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
