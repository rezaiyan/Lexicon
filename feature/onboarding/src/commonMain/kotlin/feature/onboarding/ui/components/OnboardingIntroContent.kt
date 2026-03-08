package feature.onboarding.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.graphicsLayer
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
