package overlay.fullscreen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import expects.BackHandler
import expects.OverrideSystemBars
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import overlay.LocalIsTopmostOverlay
import overlay.Overlay
import overlay.OverlayNavigator
import theme.Theme

private enum class SheetValue { Open, Dismissed }

/**
 * Full-screen overlay that visually resembles a bottom sheet but is NOT a ModalBottomSheet.
 *
 * Use this instead of [overlay.bottomsheet.BottomSheetOverlay] with [overlay.bottomsheet.BottomSheetMode.FullScreen]
 * when the content needs to host its own ModalBottomSheet children (e.g. an edit sheet on top of
 * a review screen). Nesting ModalBottomSheet causes UX issues on some platforms.
 */
class FullScreenOverlay(
    private val properties: FullScreenProperties = FullScreenProperties(),
    private val content: @Composable (OverlayNavigator) -> Unit
) : Overlay {

    @Composable
    override fun Content(navigator: OverlayNavigator) {
        val coroutineScope = rememberCoroutineScope()
        val motion = Theme.motion

        // Enter/exit animation state
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = if (visible)
                tween(motion.durationMedium2, easing = motion.easingDecelerate)
            else
                tween(motion.durationMedium, easing = motion.easingAccelerate),
            label = "fullScreenAlpha"
        )
        val enterTranslationY by animateFloatAsState(
            targetValue = if (visible) 0f else 300f,
            animationSpec = if (visible)
                tween(motion.durationLong, easing = motion.easingDecelerate)
            else
                tween(motion.durationMedium, easing = motion.easingAccelerate),
            label = "fullScreenSlide"
        )

        var isDismissing by remember { mutableStateOf(false) }

        // Normal dismiss (back press / close button): original fade + slide animation
        val animatedDismiss: () -> Unit = {
            if (!isDismissing) {
                isDismissing = true
                visible = false
                coroutineScope.launch {
                    delay(motion.durationMedium.toLong())
                    navigator.dismiss()
                }
            }
        }

        // Swipe-to-dismiss: AnchoredDraggableState for natural bottom-sheet feel.
        // The gesture coroutine stays alive throughout the drag so fling physics
        // kick in immediately on release — no coroutine scheduling gap.
        val swipeState = remember { AnchoredDraggableState(initialValue = SheetValue.Open) }

        // Configures physics on the state and returns a fling behavior for anchoredDraggable.
        // positionalThreshold: 40% of sheet height triggers dismiss on release.
        // snapAnimationSpec: spring used when settling to an anchor (spring-back or snap-dismiss).
        val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
            state = swipeState,
            positionalThreshold = { totalDistance -> totalDistance * 0.4f },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )

        // Dismiss once the sheet fully settles at the Dismissed anchor.
        // settledValue (not currentValue) — currentValue updates mid-drag to the nearest anchor,
        // which would dismiss while the finger is still on screen.
        LaunchedEffect(swipeState.settledValue) {
            if (swipeState.settledValue == SheetValue.Dismissed && !isDismissing) {
                isDismissing = true
                navigator.dismiss()
            }
        }

        val isTopMost = LocalIsTopmostOverlay.current
        BackHandler(enabled = isTopMost) {
            if (properties.dismissOnBackPress) animatedDismiss()
        }

        // System bars: match sheet surface color
        val surfaceColor = MaterialTheme.colorScheme.surface
        OverrideSystemBars(
            statusBarColor = surfaceColor,
            navigationBarColor = surfaceColor,
            darkIcons = surfaceColor.luminance() > 0.5f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    swipeState.updateAnchors(
                        DraggableAnchors {
                            SheetValue.Open at 0f
                            SheetValue.Dismissed at size.height.toFloat()
                        }
                    )
                }
                .graphicsLayer {
                    this.alpha = alpha
                    val swipeOffset = if (swipeState.offset.isNaN()) 0f else swipeState.offset
                    this.translationY = enterTranslationY + swipeOffset
                }
                .background(surfaceColor)
                .then(
                    if (properties.dismissOnSwipe && isTopMost && !isDismissing)
                        Modifier.anchoredDraggable(
                            state = swipeState,
                            orientation = Orientation.Vertical,
                            flingBehavior = flingBehavior,
                        )
                    else Modifier
                )
                .then(
                    if (properties.isStatusBarsPaddingEnabled)
                        Modifier.statusBarsPadding()
                    else Modifier
                )
                .then(
                    if (properties.isNavigationBarsPaddingEnabled)
                        Modifier.navigationBarsPadding()
                    else Modifier
                )
        ) {
            content(OverlayNavigator { animatedDismiss() })
        }
    }
}
