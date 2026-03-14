package components.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import theme.AppColors

private val ConfettiColors = listOf(
    AppColors.primary,          // purple
    AppColors.accentLavender,   // light purple
    AppColors.secondary,        // green
    AppColors.accentEmerald,    // emerald
    AppColors.tertiary,         // orange
    AppColors.accentAmber,      // warning yellow
    AppColors.error,            // red
    AppColors.accentPink,       // pink
    AppColors.accentSkyBlue,    // info blue
    AppColors.accentIndigo,     // indigo
)

private data class ConfettiParticle(
    val x: Float,          // normalized 0..1 initial horizontal position
    val spreadX: Float,    // horizontal spread velocity (-1..1)
    val speed: Float,      // fall speed multiplier
    val delay: Float,      // normalized 0..1 start delay
    val size: Float,       // particle size in dp
    val rotation: Float,   // base rotation degrees
    val rotSpeed: Float,   // rotation speed
    val wobbleAmp: Float,  // horizontal wobble amplitude
    val wobbleFreq: Float, // wobble frequency
    val color: Color,
    val shape: ParticleShape,
)

private enum class ParticleShape { Rect, Circle, Strip, Star, Diamond }

/**
 * Full-screen confetti celebration animation.
 *
 * Particles burst from the top-center and spread outward
 * with gravity, wobble, rotation, and varied shapes (rectangles,
 * circles, streamers, stars, diamonds).
 *
 * Pure Compose Canvas — no external dependencies.
 *
 * @param particleCount Number of confetti particles
 * @param durationMs Total animation duration
 */
@Composable
fun ConfettiOverlay(
    modifier: Modifier = Modifier,
    particleCount: Int = 80,
    durationMs: Int = 3000,
) {
    val progress = remember { Animatable(0f) }
    val particles = remember {
        List(particleCount) {
            ConfettiParticle(
                x = 0.35f + Random.nextFloat() * 0.3f, // cluster near center
                spreadX = (Random.nextFloat() - 0.5f) * 2f, // spread left/right
                speed = 0.5f + Random.nextFloat() * 0.7f,
                delay = Random.nextFloat() * 0.15f, // tighter burst window
                size = 5f + Random.nextFloat() * 9f,
                rotation = Random.nextFloat() * 360f,
                rotSpeed = 180f + Random.nextFloat() * 360f,
                wobbleAmp = 0.015f + Random.nextFloat() * 0.035f,
                wobbleFreq = 1f + Random.nextFloat() * 3f,
                color = ConfettiColors[Random.nextInt(ConfettiColors.size)],
                shape = ParticleShape.entries[Random.nextInt(ParticleShape.entries.size)],
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMs, easing = LinearEasing),
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = progress.value

        particles.forEach { p ->
            val localT = ((t - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (localT <= 0f) return@forEach

            // Vertical: accelerating fall (gravity simulation)
            val gravity = localT * localT
            val y = -p.size + (h + p.size * 2) * gravity * p.speed

            // Horizontal: spread from center + sine wobble
            val spread = p.spreadX * localT * w * 0.4f
            val wobble = sin(localT * p.wobbleFreq * 2 * PI).toFloat() * p.wobbleAmp * w
            val x = p.x * w + spread + wobble

            // Fade out near the bottom
            val alpha = when {
                localT > 0.75f -> 1f - ((localT - 0.75f) / 0.25f)
                localT < 0.05f -> localT / 0.05f // quick fade in
                else -> 1f
            }

            val rotation = p.rotation + p.rotSpeed * localT

            drawConfettiParticle(
                particle = p,
                center = Offset(x, y),
                rotation = rotation,
                alpha = alpha,
            )
        }
    }
}

private fun DrawScope.drawConfettiParticle(
    particle: ConfettiParticle,
    center: Offset,
    rotation: Float,
    alpha: Float,
) {
    val color = particle.color.copy(alpha = alpha.coerceIn(0f, 1f))
    val s = particle.size

    rotate(degrees = rotation, pivot = center) {
        when (particle.shape) {
            ParticleShape.Rect -> drawRect(
                color = color,
                topLeft = Offset(center.x - s / 2, center.y - s / 2),
                size = Size(s, s),
            )
            ParticleShape.Circle -> drawCircle(
                color = color,
                radius = s / 2,
                center = center,
            )
            ParticleShape.Strip -> drawRect(
                color = color,
                topLeft = Offset(center.x - s / 4, center.y - s),
                size = Size(s / 2, s * 2),
            )
            ParticleShape.Star -> {
                val path = starPath(center, outerRadius = s, innerRadius = s * 0.4f, points = 4)
                drawPath(path, color = color)
            }
            ParticleShape.Diamond -> {
                val path = Path().apply {
                    moveTo(center.x, center.y - s)
                    lineTo(center.x + s * 0.6f, center.y)
                    lineTo(center.x, center.y + s)
                    lineTo(center.x - s * 0.6f, center.y)
                    close()
                }
                drawPath(path, color = color)
            }
        }
    }
}

private fun starPath(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    points: Int,
): Path {
    val path = Path()
    val angleStep = PI.toFloat() / points

    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val angle = -PI.toFloat() / 2 + i * angleStep
        val x = center.x + cos(angle) * radius
        val y = center.y + sin(angle) * radius
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
