package feature.onboarding.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import components.LottieMotionIcon
import kotlinx.coroutines.delay
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.onboarding_generating_vocabulary
import lexicon.resources.generated.resources.onboarding_loading_tip_1
import lexicon.resources.generated.resources.onboarding_loading_tip_2
import lexicon.resources.generated.resources.onboarding_loading_tip_3
import lexicon.resources.generated.resources.onboarding_loading_tip_4
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private const val AI_LOADING_LOTTIE_URL =
    "https://assets-v2.lottiefiles.com/a/91ccdf52-1150-11ee-b7cc-8f23ce57c5d5/zUi6h6u4zD.json"

@Composable
fun OnboardingLoadingCard() {
    val spacing = Theme.spacing
    val motion = Theme.motion
    val loadingTips = listOf(
        stringResource(Res.string.onboarding_loading_tip_1),
        stringResource(Res.string.onboarding_loading_tip_2),
        stringResource(Res.string.onboarding_loading_tip_3),
        stringResource(Res.string.onboarding_loading_tip_4),
    )
    var currentTipIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2500)
            currentTipIndex = (currentTipIndex + 1) % loadingTips.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.md)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LottieMotionIcon(url = AI_LOADING_LOTTIE_URL, modifier = Modifier.size(120.dp))
        Spacer(modifier = Modifier.height(spacing.md))
        Text(
            text = stringResource(Res.string.onboarding_generating_vocabulary),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(spacing.sm))
        AnimatedContent(
            targetState = loadingTips[currentTipIndex],
            transitionSpec = {
                (fadeIn(animationSpec = tween(motion.durationMedium2)) togetherWith
                    fadeOut(animationSpec = tween(motion.durationMedium2)))
                    .using(SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> snap() }))
            },
            label = "loading_tip",
        ) { tip ->
            Text(
                text = tip,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                minLines = 2,
            )
        }
    }
}
