package components.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * AI-style scanning overlay with an animated gradient wave that sweeps
 * vertically across the content. Designed to overlay image thumbnails
 * during AI processing.
 *
 * @param label Text shown at the bottom of the overlay.
 * @param modifier Modifier for the root Box.
 */
@Composable
fun AiScanOverlay(
    label: String,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val infiniteTransition = rememberInfiniteTransition(label = "AiScan")

    // Main scan wave position — sweeps top-to-bottom
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = -0.15f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scanWave",
    )

    // Secondary shimmer — slower, broader
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer",
    )

    // Gentle pulse for the overlay opacity
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Semi-transparent dark tint
            drawRect(color = Color.Black.copy(alpha = pulseAlpha))

            // Primary scan wave — narrow, bright gradient band
            val waveCenter = h * scanProgress
            val waveHalf = h * 0.08f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primary.copy(alpha = 0.12f),
                        primary.copy(alpha = 0.35f),
                        tertiary.copy(alpha = 0.35f),
                        tertiary.copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    startY = waveCenter - waveHalf,
                    endY = waveCenter + waveHalf,
                ),
                topLeft = Offset.Zero,
                size = Size(w, h),
            )

            // Horizontal glow line at wave center
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primary.copy(alpha = 0.5f),
                        Color.Transparent,
                    ),
                    startY = waveCenter - 1.5f.dp.toPx(),
                    endY = waveCenter + 1.5f.dp.toPx(),
                ),
                topLeft = Offset.Zero,
                size = Size(w, h),
            )

            // Secondary shimmer — broader, softer
            val shimmerCenter = h * shimmerProgress
            val shimmerHalf = h * 0.18f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.04f),
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent,
                    ),
                    startY = shimmerCenter - shimmerHalf,
                    endY = shimmerCenter + shimmerHalf,
                ),
                topLeft = Offset.Zero,
                size = Size(w, h),
            )
        }

        // Label badge at the bottom
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            shadowElevation = 4.dp,
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
