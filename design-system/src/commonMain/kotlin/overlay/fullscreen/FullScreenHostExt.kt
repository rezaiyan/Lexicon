package overlay.fullscreen

import androidx.compose.runtime.Composable
import overlay.DefaultOverlayDestination
import overlay.NavDestination
import overlay.OverlayHost
import overlay.OverlayNavigator

fun OverlayHost.showFullScreen(
    destination: NavDestination? = null,
    tag: String? = null,
    properties: FullScreenProperties = FullScreenProperties(),
    content: @Composable (OverlayNavigator) -> Unit
) {
    show(
        overlay = FullScreenOverlay(
            properties = properties,
            content = content
        ),
        destination = destination ?: DefaultOverlayDestination,
        tag = tag
    )
}
