package presentation.ui.overlay.dialog

import androidx.compose.runtime.Composable
import presentation.ui.overlay.*

/**
 * Host helper to show dialogs.
 * Accepts optional destination/tag for navigation model.
 */
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

