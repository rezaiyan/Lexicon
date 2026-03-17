package overlay.fullscreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import expects.BackHandler
import expects.OverrideSystemBars
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import overlay.LocalIsTopmostOverlay
import overlay.Overlay
import overlay.OverlayNavigator
import theme.Theme

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
        val translationY by animateFloatAsState(
            targetValue = if (visible) 0f else 300f,
            animationSpec = if (visible)
                tween(motion.durationLong, easing = motion.easingDecelerate)
            else
                tween(motion.durationMedium, easing = motion.easingAccelerate),
            label = "fullScreenSlide"
        )

        val animatedDismiss: () -> Unit = {
            if (visible) {
                visible = false
                coroutineScope.launch {
                    delay(motion.durationMedium.toLong())
                    navigator.dismiss()
                }
            }
        }

        val isTopMost = LocalIsTopmostOverlay.current
        BackHandler(enabled = isTopMost) {
            if (properties.dismissOnBackPress) {
                animatedDismiss()
            }
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
                .graphicsLayer {
                    this.alpha = alpha
                    this.translationY = translationY
                }
                .background(surfaceColor)
                .then(
                    if (properties.isNavigationBarsPaddingEnabled)
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
