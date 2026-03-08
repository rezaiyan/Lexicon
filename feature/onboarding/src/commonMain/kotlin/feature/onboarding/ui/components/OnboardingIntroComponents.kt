package feature.onboarding.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import theme.Theme

internal val HeroOuterGlowSize = 140.dp
internal val HeroInnerCircleSize = 92.dp

@Composable
internal fun HeroLogo(animationStarted: Boolean) {
    val motion = Theme.motion

    // Entrance: spring scale from small to full size
    val heroScale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "hero_scale"
    )
    val heroAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(
            durationMillis = motion.durationXLong,
            easing = motion.easingDecelerate
        ),
        label = "hero_alpha"
    )

    // Breathing pulse on the outer glow
    val infiniteTransition = rememberInfiniteTransition(label = "hero_glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.graphicsLayer {
            scaleX = heroScale
            scaleY = heroScale
            alpha = heroAlpha
        }
    ) {
        // Outer soft glow with breathing pulse
        Box(
            modifier = Modifier
                .size(HeroOuterGlowSize)
                .graphicsLayer {
                    scaleX = glowScale
                    scaleY = glowScale
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Inner gradient circle
        Box(
            modifier = Modifier
                .size(HeroInnerCircleSize)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSizeHuge),
                tint = Color.White
            )
        }
    }
}

@Composable
internal fun FeatureItemEntry(
    index: Int,
    animationStarted: Boolean,
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    val motion = Theme.motion
    val staggerDelay = motion.durationXLong + index * motion.durationShort

    val itemAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(
            durationMillis = motion.durationLong,
            delayMillis = staggerDelay,
            easing = motion.easingDecelerate
        ),
        label = "feature_alpha_$index"
    )
    val itemOffsetY by animateFloatAsState(
        targetValue = if (animationStarted) 0f else 24f,
        animationSpec = tween(
            durationMillis = motion.durationLong,
            delayMillis = staggerDelay,
            easing = motion.easingDecelerate
        ),
        label = "feature_offset_$index"
    )

    IntroFeatureItem(
        icon = icon,
        title = title,
        description = description,
        iconTint = color,
        iconBackground = color.copy(alpha = 0.12f),
        modifier = Modifier.graphicsLayer {
            alpha = itemAlpha
            translationY = itemOffsetY
        }
    )
}

@Composable
private fun IntroFeatureItem(
    icon: ImageVector,
    title: String,
    description: String,
    iconTint: Color,
    iconBackground: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Theme.dimensions.touchTarget)
                .clip(RoundedCornerShape(Theme.shapes.medium))
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSize),
                tint = iconTint
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
