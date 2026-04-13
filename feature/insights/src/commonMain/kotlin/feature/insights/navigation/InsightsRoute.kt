package feature.insights.navigation

import androidx.compose.material3.SnackbarHostState
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
    snackbarHostState: SnackbarHostState,
) {
    composable<InsightsRoute> {
        InsightsScreen(
            onNavigateBack = onNavigateBack,
            onShowLeaderboard = onShowLeaderboard,
            onNavigateToNotificationSettings = onNavigateToNotificationSettings,
            snackbarHostState = snackbarHostState,
        )
    }
}

fun OverlayHost.showInsightsSheet(
    onShowLeaderboard: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    snackbarHostState: SnackbarHostState,
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
            snackbarHostState = snackbarHostState,
        )
    }
}
