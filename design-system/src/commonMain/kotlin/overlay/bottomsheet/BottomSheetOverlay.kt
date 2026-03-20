package overlay.bottomsheet

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.RectangleShape
import expects.BackHandler
import expects.OverrideSystemBars
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import overlay.LocalIsTopmostOverlay
import overlay.Overlay
import overlay.OverlayNavigator
import theme.Theme

class BottomSheetOverlay(
    private val mode: BottomSheetMode,
    private var properties: BottomSheetProperties = BottomSheetProperties(),
    private val content: @Composable BottomSheetOverlayScope.(OverlayNavigator) -> Unit
) : Overlay {

    private class ScopeImpl(initial: BottomSheetProperties) : BottomSheetOverlayScope {
        override var properties by mutableStateOf(initial)
    }

    @Composable
    override fun Content(navigator: OverlayNavigator) {
        val scopeImpl = remember { ScopeImpl(properties) }
        val coroutineScope = rememberCoroutineScope()

        val sheetState = rememberSheetStateFor(
            mode = mode,
            gesturesEnabled = scopeImpl.properties.sheetGesturesEnabled
        )

        // Content enter/exit animation — fade + slide for smooth open/close
        val motion = Theme.motion
        var contentVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { contentVisible = true }

        val contentAlpha by animateFloatAsState(
            targetValue = if (contentVisible) 1f else 0f,
            animationSpec = if (contentVisible)
                tween(motion.durationMedium2, delayMillis = motion.durationXShort, easing = motion.easingDecelerate)
            else
                tween(motion.durationMedium, easing = motion.easingAccelerate),
            label = "sheetContentAlpha"
        )
        val contentTranslationY by animateFloatAsState(
            targetValue = if (contentVisible) 0f else 60f,
            animationSpec = if (contentVisible)
                tween(motion.durationLong, delayMillis = motion.durationXShort, easing = motion.easingDecelerate)
            else
                tween(motion.durationMedium, easing = motion.easingAccelerate),
            label = "sheetContentSlide"
        )

        // Animated dismiss: content fades out, then overlay is removed.
        // We call navigator.dismiss() directly instead of sheetState.hide()
        // because confirmValueChange blocks hide() when gestures are disabled.
        val animatedHide: () -> Unit = {
            if (contentVisible) {
                contentVisible = false
                coroutineScope.launch {
                    delay(motion.durationMedium.toLong())
                    navigator.dismiss()
                }
            }
        }

        val isTopMost = LocalIsTopmostOverlay.current
        BackHandler(enabled = isTopMost && sheetState.isVisible) {
            if (scopeImpl.properties.dismissOnBackPress) {
                animatedHide()
            }
        }

        LaunchedEffect(Unit) {
            if (sheetState.currentValue == SheetValue.Hidden) {
                sheetState.show()
            }
        }

        LaunchedEffect(sheetState.targetValue, sheetState.isVisible) {
            if (sheetState.targetValue == SheetValue.Hidden && !sheetState.isVisible) {
                navigator.dismiss()
            }
        }

        val isFullScreen = mode == BottomSheetMode.FullScreen

        // Activity-level scrim: ModalBottomSheet draws its scrim inside a Popup
        // window that doesn't extend behind system bars. We draw our own scrim
        // here (in the activity composition) so it covers the full screen
        // including behind status bar and navigation bar.
        val sheetVisible = sheetState.targetValue != SheetValue.Hidden
        val scrimAlpha by animateFloatAsState(
            targetValue = if (sheetVisible) 0.32f else 0f,
            animationSpec = tween(300)
        )
        if (scrimAlpha > 0f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)))
        }

        // Global system bar management for ALL bottom sheet modes.
        // Non-fullscreen: transparent bars so scrim shows through.
        // Fullscreen: bars match sheet surface color.
        if (sheetVisible || scrimAlpha > 0f) {
            if (isFullScreen) {
                val surfaceColor = MaterialTheme.colorScheme.surface
                OverrideSystemBars(
                    statusBarColor = surfaceColor,
                    navigationBarColor = surfaceColor,
                    darkIcons = surfaceColor.luminance() > 0.5f
                )
            } else {
                OverrideSystemBars(
                    statusBarColor = Color.Transparent,
                    navigationBarColor = Color.Transparent,
                    darkIcons = false
                )
            }
        }

        // Wrap navigator so programmatic dismiss() goes through animated exit
        val animatedNavigator = OverlayNavigator { animatedHide() }

        ModalBottomSheet(
            onDismissRequest = {
                if (scopeImpl.properties.dismissOnTouchOutside) {
                    animatedHide()
                }
            },
            dragHandle = if (isFullScreen || !scopeImpl.properties.showDragHandle) {
                null
            } else {
                { BottomSheetDefaults.DragHandle() }
            },
            sheetState = sheetState,
            sheetGesturesEnabled = scopeImpl.properties.sheetGesturesEnabled,
            shape = if (isFullScreen) RectangleShape else BottomSheetDefaults.ExpandedShape,
            containerColor = if (isFullScreen) {
                MaterialTheme.colorScheme.surface
            } else {
                BottomSheetDefaults.ContainerColor
            },
            scrimColor = Color.Transparent, // Disabled — using activity-level scrim above
        ) {
            Box(
                modifier = Modifier
                    .then(
                        if (isFullScreen) {
                            if (scopeImpl.properties.isNavigationBarsPaddingEnabled)
                                Modifier.navigationBarsPadding()
                            else Modifier
                        } else {
                            Modifier.padding(Theme.spacing.md)
                        }
                    )
                    .graphicsLayer {
                        alpha = contentAlpha
                        translationY = contentTranslationY
                    }
            ) {
                val internalScope = rememberBottomSheetScopeImpl(
                    showDragHandle = (mode != BottomSheetMode.FullScreen) && scopeImpl.properties.showDragHandle,
                ) {
                    animatedHide()
                }

                CompositionLocalProvider(
                    LocalBottomSheetScope provides internalScope,
                    LocalBottomSheetOverlayScope provides scopeImpl,
                ) {
                    scopeImpl.content(animatedNavigator)
                }
            }
        }
    }
}

@Composable
private fun rememberSheetStateFor(
    mode: BottomSheetMode,
    gesturesEnabled: Boolean
): SheetState {
    val skipPartiallyExpanded = when (mode) {
        BottomSheetMode.Dynamic -> false
        BottomSheetMode.SizeToFit, BottomSheetMode.FullScreen -> true
    }

    // Use rememberUpdatedState so the lambda captures a stable State reference
    // instead of the raw Boolean. This prevents rememberModalBottomSheetState
    // from seeing a new lambda each recomposition (which would recreate SheetState).
    val currentGesturesEnabled = rememberUpdatedState(gesturesEnabled)
    val confirmValueChange = remember<(SheetValue) -> Boolean> {
        { newValue ->
            if (!currentGesturesEnabled.value) {
                newValue != SheetValue.Hidden
            } else true
        }
    }

    return rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded,
        confirmValueChange = confirmValueChange
    )
}

private fun rememberBottomSheetScopeImpl(
    showDragHandle: Boolean,
    onDismiss: () -> Unit
): BottomSheetScope = object : BottomSheetScope {
    override val isDragHandleShown: Boolean = showDragHandle
    override fun dismiss() = onDismiss()
}
