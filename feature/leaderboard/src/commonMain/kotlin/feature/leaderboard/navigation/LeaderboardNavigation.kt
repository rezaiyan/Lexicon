package feature.leaderboard.navigation

import feature.leaderboard.ui.LeaderboardScreen
import overlay.OverlayHost
import overlay.bottomsheet.BottomSheetProperties
import overlay.bottomsheet.showFullscreenBottomSheet

fun OverlayHost.showLeaderboard() {
    showFullscreenBottomSheet(
        tag = "leaderboard",
        properties = BottomSheetProperties(
            dismissOnTouchOutside = false,
            dismissOnBackPress = false,
            sheetGesturesEnabled = false,
        )
    ) { navigator ->
        LeaderboardScreen(
            onDismiss = { navigator.dismiss() }
        )
    }
}
