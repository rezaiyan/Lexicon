package feature.onboarding.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.onboarding_generating_vocabulary
import lexicon.resources.generated.resources.onboarding_loading_tip_1
import lexicon.resources.generated.resources.onboarding_loading_tip_2
import lexicon.resources.generated.resources.onboarding_loading_tip_3
import lexicon.resources.generated.resources.onboarding_loading_tip_4
import org.jetbrains.compose.resources.stringResource
import theme.Theme

@Composable
fun OnboardingLoadingCard() {
    val spacing = Theme.spacing
    var currentTipIndex by remember { mutableStateOf(0) }
    val loadingTips = listOf(
        stringResource(Res.string.onboarding_loading_tip_1),
        stringResource(Res.string.onboarding_loading_tip_2),
        stringResource(Res.string.onboarding_loading_tip_3),
        stringResource(Res.string.onboarding_loading_tip_4)
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            currentTipIndex = (currentTipIndex + 1) % loadingTips.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "loading_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_animation"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_animation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(Theme.shapes.extraLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = Theme.elevation.medium)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.lg)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer { rotationZ = rotation },
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Text(
                    text = stringResource(Res.string.onboarding_generating_vocabulary),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
                AnimatedContent(
                    targetState = loadingTips[currentTipIndex],
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "tip_animation"
                ) { tip ->
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.CenterHorizontally)
            ) {
                repeat(4) { index ->
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 1000,
                                delayMillis = index * 200,
                                easing = LinearEasing
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(Theme.spacing.xs)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}
