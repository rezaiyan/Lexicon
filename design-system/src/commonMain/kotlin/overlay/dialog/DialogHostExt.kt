package overlay.dialog

import androidx.compose.runtime.Composable
import overlay.*

fun OverlayHost.showDialog(
    destination: NavDestination? = null,
    tag: String? = null,
    properties: DialogProperties = DialogProperties(),
    content: @Composable DialogOverlayScope.(OverlayNavigator) -> Unit
) {
    show(
        overlay = DialogOverlay(
            properties = properties,
            content = content
        ),
        destination = destination ?: DefaultOverlayDestination,
        tag = tag
    )
}
