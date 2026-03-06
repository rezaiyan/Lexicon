package overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember

val LocalIsTopmostOverlay = compositionLocalOf { false }

@Composable
fun OverlayHostContainer(
    content: @Composable () -> Unit
) {
    val overlayExternalDismiss = remember {
        object : OverlayExternalDismiss {
            override fun dismiss(overlay: Overlay) {
            }
        }
    }
    val host = rememberOverlayHost(overlayExternalDismiss)
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

        val overlayData = host.currentOverlayData
        val listSize = overlayData.size

        overlayData.forEachIndexed { index, overlayDataItem ->
            val isTopMost = index == listSize - 1
            key(overlayDataItem.overlay, overlayDataItem.tag, index) {
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
