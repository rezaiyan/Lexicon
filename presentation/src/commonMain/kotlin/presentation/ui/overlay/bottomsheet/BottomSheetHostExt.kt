package presentation.ui.overlay.bottomsheet

import androidx.compose.runtime.Composable
import presentation.ui.overlay.*

/**
 * Host helpers to show common bottom sheet flavors.
 * All accept optional destination/tag for your nav model.
 */
fun OverlayHost.showFullscreenBottomSheet(
    destination: NavDestination? = null,
    tag: String? = null,
    properties: BottomSheetProperties = BottomSheetProperties(),
    content: @Composable BottomSheetOverlayScope.(OverlayNavigator) -> Unit
) {
    show(
        overlay = BottomSheetOverlay(
            mode = BottomSheetMode.FullScreen,
            properties = properties,
            content = content
        ),
        destination = destination ?: DefaultOverlayDestination,
        tag = tag
    )
}

fun OverlayHost.showDynamicBottomSheet(
    destination: NavDestination? = null,
    tag: String? = null,
    properties: BottomSheetProperties = BottomSheetProperties(),
    content: @Composable BottomSheetOverlayScope.(OverlayNavigator) -> Unit
) {
    show(
        overlay = BottomSheetOverlay(
            mode = BottomSheetMode.Dynamic,
            properties = properties,
            content = content
        ),
        destination = destination ?: DefaultOverlayDestination,
        tag = tag
    )
}

fun OverlayHost.showSizeToFitBottomSheet(
    destination: NavDestination? = null,
    tag: String? = null,
    properties: BottomSheetProperties = BottomSheetProperties(),
    content: @Composable BottomSheetOverlayScope.(OverlayNavigator) -> Unit
) {
    show(
        overlay = BottomSheetOverlay(
            mode = BottomSheetMode.SizeToFit,
            properties = properties,
            content = content
        ),
        destination = destination ?: DefaultOverlayDestination,
        tag = tag
    )
}

