package feature.study.ui.review

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import components.CounterPill
import components.GradientProgressBar
import domain.tts.model.TtsSettings
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.auto_play
import lexicon.resources.generated.resources.close
import lexicon.resources.generated.resources.tts_playback_speed
import org.jetbrains.compose.resources.stringResource
import theme.Theme

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

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Theme.spacing.extraSmall2,
                    end = Theme.spacing.medium,
                    top = Theme.spacing.extraSmall3,
                    bottom = Theme.spacing.extraSmall3
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(Res.string.close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.weight(1F))

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
            Spacer(Modifier.size(Theme.spacing.md))

            CounterPill(text = "${currentIndex + 1} / $totalCount")
        }

        GradientProgressBar(
            progress = progress,
            gradientColors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary
            ),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            animationDurationMs = 350
        )
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
            text = formatSpeed(sliderValue) + "x",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun formatSpeed(speed: Float): String {
    val rounded = kotlin.math.round(speed * 10) / 10.0
    val whole = rounded.toLong()
    val decimal = kotlin.math.round((rounded - whole) * 10).toInt()
    return "$whole.$decimal"
}

/**
 * Auto-play toggle with morphing icon animation.
 *
 * OFF → muted VolumeOff icon in a transparent circle.
 * ON  → VolumeUp icon in a filled primaryContainer circle with a subtle breathing
 *       pulse that signals "active — will pronounce each card automatically".
 */
@Composable
private fun AutoPlayToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.primaryContainer
        else Color.Transparent,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "autoPlayBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "autoPlayContent"
    )

    // Breathing pulse — always cycling, gated so it fades out when disabled
    val infiniteTransition = rememberInfiniteTransition(label = "autoPlayBreath")
    val breathCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathCycle"
    )
    val breathGate by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = tween(300),
        label = "breathGate"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                val pulse = 1f + breathCycle * 0.06f * breathGate
                scaleX = pulse
                scaleY = pulse
            }
            .clip(CircleShape)
            .background(bgColor)
            .semantics {
                role = Role.Switch
                contentDescription = "Auto-play pronunciation"
                stateDescription = if (enabled) "On" else "Off"
                toggleableState = if (enabled) ToggleableState.On else ToggleableState.Off
            }
            .clickable(role = Role.Switch) { onToggle(!enabled) }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = enabled,
            transitionSpec = {
                (scaleIn(
                    initialScale = 0.6f,
                    animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f)
                ) + fadeIn(tween(150)))
                    .togetherWith(
                        scaleOut(
                            targetScale = 0.6f,
                            animationSpec = tween(150)
                        ) + fadeOut(tween(100))
                    )
                    .using(SizeTransform(clip = false))
            },
            label = "autoPlayIcon"
        ) { isEnabled ->
            Icon(
                imageVector = if (isEnabled) Icons.AutoMirrored.Filled.VolumeUp
                else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = stringResource(Res.string.auto_play),
                modifier = Modifier.size(22.dp),
                tint = contentColor
            )
        }
    }
}
