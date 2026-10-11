package feature.support.navigation

import feature.support.ui.SupportScreen
import overlay.OverlayHost
import overlay.bottomsheet.BottomSheetProperties
import overlay.bottomsheet.showFullscreenBottomSheet

fun OverlayHost.showSupport() {
    showFullscreenBottomSheet(
        tag = "support",
        properties = BottomSheetProperties(
            dismissOnTouchOutside = false,
            dismissOnBackPress = true,
            sheetGesturesEnabled = false,
        )
    ) { navigator ->
        SupportScreen(onDismiss = { navigator.dismiss() })
    }
}
