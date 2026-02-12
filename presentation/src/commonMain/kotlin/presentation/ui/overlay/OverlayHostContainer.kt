package presentation.ui.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember

/**
 * CompositionLocal to indicate if an overlay is the topmost in the stack.
 * Only the topmost overlay should handle back presses.
 */
val LocalIsTopmostOverlay = compositionLocalOf { false }

@Composable
fun OverlayHostContainer(
    content: @Composable () -> Unit
) {
    val overlayExternalDismiss = remember {
        object : OverlayExternalDismiss {
            override fun dismiss(overlay: Overlay) {
                // This will be handled by OverlayHostContainer which bridges to the host
            }
        }
    }
    val host = rememberOverlayHost(overlayExternalDismiss)
    // Bridge: when an overlay wants to dismiss, remove it from host.
    val externalDismiss = remember(host) {
        object : OverlayExternalDismiss {
            override fun dismiss(overlay: Overlay) {
                val toRemove = host.currentOverlayData.filter { it.overlay == overlay }
                toRemove.forEach { host.currentOverlayData.remove(it) }
            }
        }
    }

    CompositionLocalProvider(LocalOverlayHost provides host) {
        content()

        // Render all overlays in the stack - this allows dialogs to appear on top of bottom sheets
        // Material3's ModalBottomSheet and AlertDialog naturally stack correctly when both are in composition
        val overlayData = host.currentOverlayData
        val listSize = overlayData.size

        // Render all overlays in order - later items (dialogs) will appear on top of earlier ones (bottom sheets)
        // Use overlay instance identity as key to ensure each instance is treated separately
        overlayData.forEachIndexed { index, overlayDataItem ->
            val isTopMost = index == listSize - 1
            // Use a combination of overlay instance identity and index for unique key
            key(overlayDataItem.overlay, overlayDataItem.tag, index) {
                // Provide isTopMost via CompositionLocal so overlays can control back press behavior
                CompositionLocalProvider(LocalIsTopmostOverlay provides isTopMost) {
                    overlayDataItem.overlay.Content(
                        navigator = {
                            externalDismiss.dismiss(overlayDataItem.overlay)
                        }
                    )
                }
            }
        }
    }
}

