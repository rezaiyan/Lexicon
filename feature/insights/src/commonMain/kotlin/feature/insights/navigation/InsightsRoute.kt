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
) {
    composable<InsightsRoute> {
        InsightsScreen(onNavigateBack = onNavigateBack)
    }
}

fun OverlayHost.showInsightsSheet() {
    showFullScreen(
        tag = "insights",
        properties = FullScreenProperties(
            dismissOnBackPress = true,
            isStatusBarsPaddingEnabled = false,
        ),
    ) { navigator ->
        InsightsScreen(onNavigateBack = { navigator.dismiss() })
    }
}
