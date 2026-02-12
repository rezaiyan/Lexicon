package presentation.ui.overlay

import androidx.compose.runtime.Composable

/**
 * Minimal overlay contract.
 */
interface Overlay {
    @Composable
    fun Content(navigator: OverlayNavigator)
}

/**
 * Navigator for dismissing overlays.
 */
fun interface OverlayNavigator {
    fun dismiss()
}

