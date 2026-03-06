package overlay

import androidx.compose.runtime.Composable

interface Overlay {
    @Composable
    fun Content(navigator: OverlayNavigator)
}

fun interface OverlayNavigator {
    fun dismiss()
}
