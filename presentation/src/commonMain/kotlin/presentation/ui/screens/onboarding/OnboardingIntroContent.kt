package presentation.ui.screens.onboarding

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.app_name
import lexicon.resources.generated.resources.onboarding_get_started
import lexicon.resources.generated.resources.onboarding_personalize_subtitle
import lexicon.resources.generated.resources.onboarding_personalized_setup
import lexicon.resources.generated.resources.onboarding_personalized_setup_desc
import lexicon.resources.generated.resources.onboarding_smart_learning
import lexicon.resources.generated.resources.onboarding_smart_learning_desc
import lexicon.resources.generated.resources.onboarding_start_blank
import lexicon.resources.generated.resources.onboarding_starter_vocabulary_feature
import lexicon.resources.generated.resources.onboarding_starter_vocabulary_desc
import lexicon.resources.generated.resources.onboarding_welcome_to
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private val HeroOuterGlowSize = 140.dp
private val HeroInnerCircleSize = 92.dp
private val SubtitleMaxWidth = 300.dp
private val ButtonContentPaddingVertical = 14.dp
private val ButtonContentPaddingHorizontal = 24.dp

@Composable
internal fun OnboardingIntroContent(
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val spacing = Theme.spacing
    val dimensions = Theme.dimensions
    val motion = Theme.motion

    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationStarted = true }

    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HeroLogo(animationStarted = animationStarted)

            Spacer(modifier = Modifier.height(spacing.xl))

            // Welcome text — fades in after the hero lands
            val textAlpha by animateFloatAsState(
                targetValue = if (animationStarted) 1f else 0f,
                animationSpec = tween(
                    durationMillis = motion.durationXLong,
                    delayMillis = motion.durationMedium,
                    easing = motion.easingDecelerate
                ),
                label = "text_alpha"
            )

            Column(
                modifier = Modifier.graphicsLayer { alpha = textAlpha },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(Res.string.onboarding_welcome_to),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                Text(
                    text = stringResource(Res.string.onboarding_personalize_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = SubtitleMaxWidth)
                )
            }

            Spacer(modifier = Modifier.height(spacing.xl))

            // Feature items — staggered slide-up entrance
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                FeatureItemEntry(
                    index = 0,
                    animationStarted = animationStarted,
                    icon = Icons.Default.Settings,
                    title = stringResource(Res.string.onboarding_personalized_setup),
                    description = stringResource(Res.string.onboarding_personalized_setup_desc),
                    color = MaterialTheme.colorScheme.primary
                )
                FeatureItemEntry(
                    index = 1,
                    animationStarted = animationStarted,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    title = stringResource(Res.string.onboarding_starter_vocabulary_feature),
                    description = stringResource(Res.string.onboarding_starter_vocabulary_desc),
                    color = MaterialTheme.colorScheme.secondary
                )
                FeatureItemEntry(
                    index = 2,
                    animationStarted = animationStarted,
                    icon = Icons.Default.School,
                    title = stringResource(Res.string.onboarding_smart_learning),
                    description = stringResource(Res.string.onboarding_smart_learning_desc),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // Bottom CTA — fades in last
        val ctaAlpha by animateFloatAsState(
            targetValue = if (animationStarted) 1f else 0f,
            animationSpec = tween(
                durationMillis = motion.durationLong,
                delayMillis = motion.durationXXLong,
                easing = motion.easingDecelerate
            ),
            label = "cta_alpha"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg)
                .graphicsLayer { alpha = ctaAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.xxs)
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .padding(top = spacing.md)
                    .fillMaxWidth()
                    .widthIn(max = dimensions.contentMaxWidth),
                contentPadding = PaddingValues(
                    vertical = ButtonContentPaddingVertical,
                    horizontal = ButtonContentPaddingHorizontal
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(Theme.shapes.pill)
            ) {
                Text(
                    stringResource(Res.string.onboarding_get_started),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.size(spacing.xs))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(dimensions.iconSizeMedium)
                )
            }

            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = dimensions.contentMaxWidth)
            ) {
                Text(
                    stringResource(Res.string.onboarding_start_blank),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(spacing.lg))
        }
    }
}

@Composable
private fun HeroLogo(animationStarted: Boolean) {
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
private fun FeatureItemEntry(
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
