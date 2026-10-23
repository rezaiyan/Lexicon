package feature.insights.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import domain.word.model.ReviewSource
import feature.insights.coach.ui.InsightsCoachScreen
import kotlinx.serialization.Serializable

@Serializable
data object InsightsRoute

/** Reviews and Word Rush run on the Study tab, so Insights hands them off through the callbacks. */
fun NavGraphBuilder.insightsGraph(
    onShowLeaderboard: () -> Unit,
    onStartReview: (ReviewSource) -> Unit,
    onStartWordRush: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onReminderEnabled: () -> Unit,
) {
    composable<InsightsRoute> {
        InsightsCoachScreen(
            onShowLeaderboard = onShowLeaderboard,
            onStartReview = onStartReview,
            onStartWordRush = onStartWordRush,
            onOpenNotificationSettings = onNavigateToNotificationSettings,
            onReminderEnabled = onReminderEnabled,
        )
    }
}
