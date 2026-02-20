package presentation.ui.screens.study

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

/** Animates an integer from its current value to [target] with ease-out. */
@Composable
fun rememberAnimatedCounter(target: Int, durationMillis: Int = 700): Int {
    val animated by animateIntAsState(
        targetValue = target,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "counter"
    )
    return animated
}

/**
 * Oscillating scale factor for attention-grabbing elements (e.g. CTA buttons).
 * Stops automatically after [stopAfterMs] milliseconds (0 = infinite).
 */
@Composable
fun rememberPulseScale(stopAfterMs: Long = 0): Float {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (stopAfterMs > 0) {
            withTimeoutOrNull(stopAfterMs) {
                while (true) {
                    scale.animateTo(1.04f, tween(900, easing = FastOutSlowInEasing))
                    scale.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
                }
            }
            scale.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        } else {
            while (isActive) {
                scale.animateTo(1.04f, tween(900, easing = FastOutSlowInEasing))
                scale.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
            }
        }
    }
    return scale.value
}

/**
 * Staggered fade + upward slide entrance for list items.
 * Each item delays by [index] × [baseDelayMs] before animating in.
 */
fun Modifier.staggeredFadeSlide(index: Int, baseDelayMs: Int = 55): Modifier = composed {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(24f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * baseDelayMs).toLong())
        launch { alpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing)) }
        launch { offsetY.animateTo(0f, tween(320, easing = FastOutSlowInEasing)) }
    }

    this
        .alpha(alpha.value)
        .offset { IntOffset(0, offsetY.value.roundToInt()) }
}
