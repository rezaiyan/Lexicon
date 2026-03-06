package overlay.bottomsheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import expects.BackHandler
import kotlinx.coroutines.launch
import overlay.LocalIsTopmostOverlay
import overlay.Overlay
import overlay.OverlayNavigator

@OptIn(ExperimentalMaterial3Api::class)
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

        val isTopMost = LocalIsTopmostOverlay.current
        BackHandler(enabled = isTopMost && sheetState.isVisible) {
            if (scopeImpl.properties.dismissOnBackPress) {
                coroutineScope.launch { sheetState.hide() }
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

        ModalBottomSheet(
            onDismissRequest = {
                if (scopeImpl.properties.dismissOnTouchOutside) {
                    coroutineScope.launch { sheetState.hide() }
                }
            },
            dragHandle = null,
            sheetState = sheetState,
            sheetGesturesEnabled = scopeImpl.properties.sheetGesturesEnabled,
        ) {
            Box(
                modifier = Modifier
                    .then(
                        if (mode == BottomSheetMode.FullScreen &&
                            scopeImpl.properties.isNavigationBarsPaddingEnabled
                        ) Modifier.navigationBarsPadding()
                        else Modifier
                    )
            ) {
                val internalScope = rememberBottomSheetScopeImpl(
                    showDragHandle = (mode != BottomSheetMode.FullScreen)
                ) {
                    coroutineScope.launch { sheetState.hide() }
                }

                CompositionLocalProvider(LocalBottomSheetScope provides internalScope) {
                    scopeImpl.content(navigator)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberSheetStateFor(
    mode: BottomSheetMode,
    gesturesEnabled: Boolean
): SheetState {
    val skipPartiallyExpanded = when (mode) {
        BottomSheetMode.Dynamic -> false
        BottomSheetMode.SizeToFit, BottomSheetMode.FullScreen -> true
    }

    return rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded,
        confirmValueChange = { newValue ->
            if (!gesturesEnabled) {
                newValue != SheetValue.Hidden
            } else true
        }
    )
}

private fun rememberBottomSheetScopeImpl(
    showDragHandle: Boolean,
    onDismiss: () -> Unit
): BottomSheetScope = object : BottomSheetScope {
    override val isDragHandleShown: Boolean = showDragHandle
    override fun dismiss() = onDismiss()
}
