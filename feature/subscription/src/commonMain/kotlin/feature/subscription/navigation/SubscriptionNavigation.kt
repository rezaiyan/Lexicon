package feature.subscription.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import feature.subscription.SubscriptionViewModel
import feature.subscription.ui.SubscriptionScreen
import feature.subscription.ui.SubscriptionScreenActions
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object SubscriptionRoute

fun NavGraphBuilder.subscriptionGraph(
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
) {
    composable<SubscriptionRoute> {
        val subscriptionViewModel: SubscriptionViewModel = koinViewModel()
        val screenState by subscriptionViewModel.state()

        SubscriptionScreen(
            state = screenState.content,
            isPurchasing = screenState.isPurchasing,
            errorMessage = screenState.errorMessage,
            successMessage = screenState.successMessage,
            actions = SubscriptionScreenActions(
                onPurchaseClick = { pkg -> subscriptionViewModel.purchasePackage(pkg) },
                onRestoreClick = { subscriptionViewModel.restorePurchases() },
                onRetryClick = { subscriptionViewModel.retry() },
                onDismissError = { subscriptionViewModel.clearError() },
                onDismissSuccess = { subscriptionViewModel.clearSuccess() },
                onManageSubscription = { subscriptionViewModel.manageSubscription() },
                onCancelSubscription = { subscriptionViewModel.cancelSubscription() },
            ),
            snackbarHostState = snackbarHostState,
            onNavigateBack = onNavigateBack,
        )
    }
}
