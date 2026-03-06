package feature.leaderboard.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import feature.leaderboard.ui.LeaderboardScreen
import kotlinx.serialization.Serializable

@Serializable
data object LeaderboardRoute

fun NavGraphBuilder.leaderboardGraph(
    onNavigateBack: () -> Unit,
) {
    composable<LeaderboardRoute> {
        LeaderboardScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}
