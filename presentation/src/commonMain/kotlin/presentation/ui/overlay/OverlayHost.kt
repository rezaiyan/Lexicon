package presentation.ui.overlay

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * External "close this overlay now" hook used by overlay implementations.
 */
interface OverlayExternalDismiss {
    fun dismiss(overlay: Overlay)
}

/**
 * Host that owns a stack/list of overlays.
 */
interface OverlayHost {
    val currentOverlayData: SnapshotStateList<OverlayHostData>

    fun show(
        overlay: Overlay,
        destination: NavDestination = DefaultOverlayDestination,
        tag: String? = null
    )

    fun dismiss(tag: String)
    fun dismissAll()
}

/**
 * Optional routing object; keep it simple.
 */
interface NavDestination

object DefaultOverlayDestination : NavDestination

/**
 * Data class holding overlay information in the host.
 */
data class OverlayHostData(
    val overlay: Overlay,
    val destination: NavDestination,
    val tag: String?
)

/**
 * Create and remember an overlay host.
 */
@Composable
fun rememberOverlayHost(externalDismiss: OverlayExternalDismiss): OverlayHost {
    return remember(externalDismiss) { OverlayHostImpl(externalDismiss) }
}

/**
 * Implementation of OverlayHost.
 */
private class OverlayHostImpl(
    private val externalDismiss: OverlayExternalDismiss
) : OverlayHost {
    override val currentOverlayData: SnapshotStateList<OverlayHostData> =
        mutableStateListOf()

    override fun show(
        overlay: Overlay,
        destination: NavDestination,
        tag: String?
    ) {
        // Remove existing overlay with same tag to allow re-opening
        if (tag != null) {
            val existing = currentOverlayData.find { it.tag == tag }
            if (existing != null) {
                // Dismiss first, then remove from list
                // This ensures the overlay starts cleanup before we add the new one
                externalDismiss.dismiss(existing.overlay)
                currentOverlayData.remove(existing)
            }
        }

        // Add new overlay - use a fresh instance each time
        val data = OverlayHostData(overlay, destination, tag)
        currentOverlayData.add(data)
    }

    override fun dismiss(tag: String) {
        val data = currentOverlayData.find { it.tag == tag } ?: return
        externalDismiss.dismiss(data.overlay)
    }

    override fun dismissAll() {
        // Dismiss in reverse order for a natural pop behavior
        currentOverlayData.reversed().forEach { externalDismiss.dismiss(it.overlay) }
    }
}

/**
 * CompositionLocal to access the host when needed.
 */
val LocalOverlayHost = staticCompositionLocalOf<OverlayHost> {
    error("LocalOverlayHost not set")
}

