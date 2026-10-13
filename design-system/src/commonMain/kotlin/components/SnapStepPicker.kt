package components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import components.haptics.rememberHeavyHaptic
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import theme.Theme
import kotlin.math.roundToInt

// The thumb swells while held and bounces back as it lands
private const val ThumbHeldScale = 1.25f

// With nothing selected the thumb shrinks away to this before fading out
private const val ThumbHiddenScale = 0.5f

/**
 * Pick one of a few ordered steps (a level, a tone) on a chunky gradient track. The thumb follows a
 * drag, ticks lightly as it passes each stop, then lands on the nearest one with a bouncy spring
 * and a heavy haptic. A tap anywhere (track or labels) jumps to the nearest stop. Screen readers
 * get each label as a radio button.
 *
 * @param labels one per stop, in order; at least two.
 * @param selectedIndex null shows an empty track with no thumb.
 * @param onSelect called when a drag lands on a new stop, and on every tap, including a tap on the
 *   selected stop, so optional choices can treat that as clearing it.
 */
@Composable
fun SnapStepPicker(
    labels: List<String>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val steps = labels.lastIndex.coerceAtLeast(1)
    val selected = selectedIndex?.coerceIn(0, steps)
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)

    val thumbSize = Theme.dimensions.iconSizeLarge
    val trackHeight = Theme.spacing.sm
    val thumbPx = with(LocalDensity.current) { thumbSize.toPx() }
    var widthPx by remember { mutableIntStateOf(0) }

    val heavyHaptic = rememberHeavyHaptic()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val bounce = remember { spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow) }
    val position = remember { Animatable((selected ?: 0) / steps.toFloat()) }
    val thumbScale = remember { Animatable(1f) }
    var dragging by remember { mutableStateOf(false) }
    val nearest by remember { derivedStateOf { (position.value * steps).roundToInt() } }
    // Mid-drag the highlight follows the finger; otherwise it shows the selection
    val highlighted = if (dragging) nearest else selected
    val presence by animateFloatAsState(if (selected != null || dragging) 1f else 0f, bounce)

    fun fractionAt(x: Float): Float {
        val span = widthPx - thumbPx
        return if (span <= 0f) 0f else ((x - thumbPx / 2) / span).coerceIn(0f, 1f)
    }

    fun land(index: Int, tapped: Boolean) {
        heavyHaptic()
        if (tapped || index != currentSelected) currentOnSelect(index)
        scope.launch { position.animateTo(index / steps.toFloat(), bounce) }
        scope.launch {
            thumbScale.snapTo(ThumbHeldScale)
            thumbScale.animateTo(1f, bounce)
        }
    }

    // Follows a selection made elsewhere (e.g. a remembered value loading in)
    LaunchedEffect(selected) {
        if (!dragging && selected != null) position.animateTo(selected / steps.toFloat(), bounce)
    }
    // A light tick each time a drag passes a stop, so the stops are felt before letting go
    LaunchedEffect(haptics) {
        snapshotFlow { nearest }.distinctUntilChanged().drop(1).collect {
            if (dragging) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
    }

    val colors = MaterialTheme.colorScheme
    val brush = Theme.gradients.premiumHero
    // A light knob in both themes, so it reads as something to grab
    val thumbColor = if (colors.surface.luminance() > 0.5f) colors.surface else colors.onSurface
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else Theme.opacity.disabled)
            .selectableGroup()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset -> land((fractionAt(offset.x) * steps).roundToInt(), tapped = true) }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        scope.launch { thumbScale.animateTo(ThumbHeldScale, bounce) }
                        scope.launch { position.snapTo(fractionAt(offset.x)) }
                    },
                    onDragEnd = {
                        dragging = false
                        land(nearest, tapped = false)
                    },
                    onDragCancel = {
                        dragging = false
                        currentSelected?.let { land(it, tapped = false) }
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        scope.launch { position.snapTo(fractionAt(change.position.x)) }
                    },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Theme.dimensions.touchTarget)
                .onSizeChanged { widthPx = it.width }
                .drawBehind {
                    val trackPx = trackHeight.toPx()
                    val top = (size.height - trackPx) / 2
                    val radius = CornerRadius(trackPx / 2)
                    val span = size.width - thumbPx
                    val thumbCenter = thumbPx / 2 + position.value * span
                    drawRoundRect(colors.surfaceContainerHighest, Offset(0f, top), Size(size.width, trackPx), radius)
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(0f, top),
                        size = Size(thumbCenter.coerceAtLeast(trackPx), trackPx),
                        cornerRadius = radius,
                        alpha = presence.coerceIn(0f, 1f),
                    )
                    for (stop in 0..steps) {
                        val x = thumbPx / 2 + stop / steps.toFloat() * span
                        val filled = presence > 0f && x <= thumbCenter
                        drawCircle(
                            color = if (filled) colors.onPrimary else colors.outline,
                            radius = trackPx / 6,
                            center = Offset(x, size.height / 2),
                        )
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset((position.value * (widthPx - thumbPx)).roundToInt(), 0) }
                    .size(thumbSize)
                    .graphicsLayer {
                        val shown = ThumbHiddenScale + (1f - ThumbHiddenScale) * presence
                        scaleX = thumbScale.value * shown
                        scaleY = thumbScale.value * shown
                        alpha = presence.coerceIn(0f, 1f)
                    }
                    .shadow(Theme.elevation.high, CircleShape)
                    .background(thumbColor, CircleShape)
                    .border(Theme.dimensions.borderWidth, colors.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(trackHeight).background(brush, CircleShape))
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                val isHighlighted = index == highlighted
                val color by animateColorAsState(if (isHighlighted) colors.primary else colors.onSurfaceVariant)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        // The column handles touch; this gives screen readers a radio button per stop
                        .selectable(
                            selected = index == selected,
                            enabled = enabled,
                            role = Role.RadioButton,
                            interactionSource = null,
                            indication = null,
                            onClick = { land(index, tapped = true) },
                        ),
                    contentAlignment = when (index) {
                        0 -> Alignment.CenterStart
                        labels.lastIndex -> Alignment.CenterEnd
                        else -> Alignment.Center
                    },
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                        color = color,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
