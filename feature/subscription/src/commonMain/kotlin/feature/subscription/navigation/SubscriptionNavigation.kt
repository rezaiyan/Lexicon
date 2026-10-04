package feature.subscription.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import feature.subscription.ui.SubscriptionScreen
import kotlinx.serialization.Serializable

@Serializable
data object SubscriptionRoute

fun NavGraphBuilder.subscriptionGraph(
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
) {
    composable<SubscriptionRoute> {
        SubscriptionScreen(snackbarHostState = snackbarHostState, onNavigateBack = onNavigateBack)
    }
}
