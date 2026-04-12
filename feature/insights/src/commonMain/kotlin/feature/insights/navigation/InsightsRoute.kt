package feature.insights.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import feature.insights.ui.InsightsScreen
import kotlinx.serialization.Serializable
import overlay.OverlayHost
import overlay.fullscreen.FullScreenProperties
import overlay.fullscreen.showFullScreen

@Serializable
data object InsightsRoute

fun NavGraphBuilder.insightsGraph(
    onNavigateBack: () -> Unit,
    onShowLeaderboard: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
) {
    composable<InsightsRoute> {
        InsightsScreen(
            onNavigateBack = onNavigateBack,
            onShowLeaderboard = onShowLeaderboard,
            onNavigateToNotificationSettings = onNavigateToNotificationSettings,
        )
    }
}

fun OverlayHost.showInsightsSheet(
    onShowLeaderboard: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
) {
    showFullScreen(
        tag = "insights",
        properties = FullScreenProperties(
            dismissOnBackPress = true,
            dismissOnSwipe = true,
            isStatusBarsPaddingEnabled = false,
        ),
    ) { navigator ->
        InsightsScreen(
            onNavigateBack = { navigator.dismiss() },
            onShowLeaderboard = onShowLeaderboard,
            onNavigateToNotificationSettings = onNavigateToNotificationSettings,
        )
    }
}
