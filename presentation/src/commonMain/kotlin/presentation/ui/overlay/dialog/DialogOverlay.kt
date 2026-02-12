@file:OptIn(ExperimentalMaterial3Api::class)

package presentation.ui.overlay.dialog

import presentation.ui.components.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import expects.BackHandler
import presentation.ui.overlay.Overlay
import presentation.ui.overlay.OverlayNavigator

/**
 * Dialog overlay implementation.
 */
class DialogOverlay(
    private var properties: DialogProperties = DialogProperties(),
    private val content: @Composable DialogOverlayScope.(OverlayNavigator) -> Unit
) : Overlay {

    private class ScopeImpl(initial: DialogProperties) : DialogOverlayScope {
        override var properties by mutableStateOf(initial)
    }

    @Composable
    override fun Content(navigator: OverlayNavigator) {
        val scopeImpl = remember { ScopeImpl(properties) }
        var isVisible by remember { mutableStateOf(true) }

        // Back press - only handle if this is the topmost overlay
        val isTopMost = presentation.ui.overlay.LocalIsTopmostOverlay.current
        BackHandler(enabled = isTopMost && isVisible && scopeImpl.properties.dismissOnBackPress) {
            isVisible = false
            navigator.dismiss()
        }

        // Auto-dismiss when visibility changes
        LaunchedEffect(isVisible) {
            if (!isVisible) {
                navigator.dismiss()
            }
        }

        if (isVisible) {
            BasicAlertDialog(
                onDismissRequest = {
                    if (scopeImpl.properties.dismissOnTouchOutside) {
                        isVisible = false
                    }
                },
                content = {
                    scopeImpl.content(OverlayNavigator {
                        isVisible = false
                        navigator.dismiss()
                    })
                }
            )
        }
    }
}

