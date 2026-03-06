package overlay

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList

interface OverlayExternalDismiss {
    fun dismiss(overlay: Overlay)
}

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

interface NavDestination

object DefaultOverlayDestination : NavDestination

data class OverlayHostData(
    val overlay: Overlay,
    val destination: NavDestination,
    val tag: String?
)

@Composable
fun rememberOverlayHost(externalDismiss: OverlayExternalDismiss): OverlayHost {
    return remember(externalDismiss) { OverlayHostImpl(externalDismiss) }
}

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
        if (tag != null) {
            val existing = currentOverlayData.find { it.tag == tag }
            if (existing != null) {
                externalDismiss.dismiss(existing.overlay)
                currentOverlayData.remove(existing)
            }
        }

        val data = OverlayHostData(overlay, destination, tag)
        currentOverlayData.add(data)
    }

    override fun dismiss(tag: String) {
        val data = currentOverlayData.find { it.tag == tag } ?: return
        externalDismiss.dismiss(data.overlay)
    }

    override fun dismissAll() {
        currentOverlayData.reversed().forEach { externalDismiss.dismiss(it.overlay) }
    }
}

val LocalOverlayHost = staticCompositionLocalOf<OverlayHost> {
    error("LocalOverlayHost not set")
}
