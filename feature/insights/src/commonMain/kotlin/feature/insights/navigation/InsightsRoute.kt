package feature.insights.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import feature.insights.ui.InsightsScreen
import kotlinx.serialization.Serializable

@Serializable
data object InsightsRoute

fun NavGraphBuilder.insightsGraph(
    onNavigateBack: () -> Unit,
) {
    composable<InsightsRoute> {
        InsightsScreen(onNavigateBack = onNavigateBack)
    }
}
