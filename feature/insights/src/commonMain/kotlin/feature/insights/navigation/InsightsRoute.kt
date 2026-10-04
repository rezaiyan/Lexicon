package feature.insights.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import feature.insights.ui.InsightsScreen
import kotlinx.serialization.Serializable

@Serializable
data object InsightsRoute

fun NavGraphBuilder.insightsGraph(
    onShowLeaderboard: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
) {
    composable<InsightsRoute> {
        InsightsScreen(
            onShowLeaderboard = onShowLeaderboard,
            onNavigateToNotificationSettings = onNavigateToNotificationSettings,
        )
    }
}
