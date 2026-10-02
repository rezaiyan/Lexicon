package feature.study.ui.review

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import components.GradientProgressBar
import domain.tts.model.TtsSettings
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.auto_play
import lexicon.resources.generated.resources.close
import lexicon.resources.generated.resources.tts_playback_speed
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.LexiconFormatters

/**
 * Compact top bar: close button (left), session title (center), card counter chip (right).
 * A full-bleed gradient progress strip runs along the bottom edge — no horizontal padding
 * so it spans edge-to-edge and feels like a native reading indicator.
 *
 * Long-pressing the auto-play toggle opens a rich tooltip with a speech speed slider.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReviewTopBar(
    currentIndex: Int,
    totalCount: Int,
    isAutoPlayEnabled: Boolean,
    speechRate: Float,
    onAutoPlayToggle: (Boolean) -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
    onClose: () -> Unit,
) {
    val progress = (currentIndex + 1).toFloat() / totalCount.toFloat()
    val tooltipState = rememberTooltipState(isPersistent = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = Theme.spacing.xxs, end = Theme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        IconButton(onClick = onClose) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(Res.string.close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        GradientProgressBar(
            progress = progress,
            gradientColors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            ),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            height = Theme.spacing.xs,
            animationDurationMs = 350,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(Theme.shapes.pill)),
        )

        Text(
            text = "${currentIndex + 1} / $totalCount",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Theme.spacing.xxs),
        )

        TooltipBox(
            positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
            tooltip = {
                RichTooltip(
                    title = { Text(stringResource(Res.string.tts_playback_speed)) },
                ) {
                    SpeedSliderContent(
                        speechRate = speechRate,
                        onSpeechRateChanged = onSpeechRateChanged,
                    )
                }
            },
            state = tooltipState,
        ) {
            AutoPlayToggle(
                enabled = isAutoPlayEnabled,
                onToggle = onAutoPlayToggle,
            )
        }
    }
}

@Composable
private fun SpeedSliderContent(
    speechRate: Float,
    onSpeechRateChanged: (Float) -> Unit,
) {
    var sliderValue by remember(speechRate) { mutableFloatStateOf(speechRate) }

    Row(
        modifier = Modifier.width(220.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onSpeechRateChanged(sliderValue) },
            valueRange = TtsSettings.MIN_SPEECH_RATE..TtsSettings.MAX_SPEECH_RATE,
            steps = 5,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = LexiconFormatters.speed(sliderValue) + "x",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}


/**
 * Auto-play toggle: 40dp tonal circle. ON = brand tint + VolumeUp, OFF = neutral + VolumeOff.
 * Long-press (via the surrounding TooltipBox) opens the speech-speed slider.
 */
@Composable
private fun AutoPlayToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.focus)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "autoPlayBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "autoPlayContent"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bgColor)
            .semantics {
                role = Role.Switch
                contentDescription = "Auto-play pronunciation"
                stateDescription = if (enabled) "On" else "Off"
                toggleableState = if (enabled) ToggleableState.On else ToggleableState.Off
            }
            .clickable(role = Role.Switch) { onToggle(!enabled) },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = enabled,
            transitionSpec = {
                (scaleIn(
                    initialScale = 0.6f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f)
                ) + fadeIn(tween(150)))
                    .togetherWith(scaleOut(targetScale = 0.6f, animationSpec = tween(150)) + fadeOut(tween(100)))
                    .using(SizeTransform(clip = false))
            },
            label = "autoPlayIcon"
        ) { isEnabled ->
            Icon(
                imageVector = if (isEnabled) Icons.AutoMirrored.Filled.VolumeUp
                else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = stringResource(Res.string.auto_play),
                modifier = Modifier.size(20.dp),
                tint = contentColor
            )
        }
    }
}
