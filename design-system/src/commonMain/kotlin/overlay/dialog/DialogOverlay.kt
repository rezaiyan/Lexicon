package overlay.dialog

import components.dialog.BasicAlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import expects.BackHandler
import overlay.LocalIsTopmostOverlay
import overlay.Overlay
import overlay.OverlayNavigator

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

        val isTopMost = LocalIsTopmostOverlay.current
        BackHandler(enabled = isTopMost && isVisible && scopeImpl.properties.dismissOnBackPress) {
            isVisible = false
            navigator.dismiss()
        }

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
