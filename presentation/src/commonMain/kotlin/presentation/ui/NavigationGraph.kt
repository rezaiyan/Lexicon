package presentation.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel
import presentation.feature.subscription.SubscriptionViewModel
import presentation.model.TabDestination
import presentation.ui.screens.EditProfileScreen
import presentation.ui.screens.LeaderboardScreen
import presentation.ui.screens.ProfileScreen
import presentation.ui.screens.SettingsScreen
import presentation.ui.screens.StudyScreen
import presentation.ui.screens.SubscriptionScreen
import presentation.ui.screens.SubscriptionScreenActions
import presentation.ui.screens.settings.WordManagerScreen

@Composable
internal fun NavigationGraph(
    modifier: Modifier,
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = TabDestination.Study,
        modifier = modifier.fillMaxSize(),
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = {
            slideInHorizontally(animationSpec = tween(300), initialOffsetX = { -it }) +
                fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { it }) +
                fadeOut(animationSpec = tween(300))
        }
    ) {
        composable<TabDestination.Profile> {
            ProfileScreen(
                onNavigateToLeaderboard = {
                    navController.navigate(TabDestination.Leaderboard)
                },
                onNavigateToEditProfile = {
                    navController.navigate(TabDestination.EditProfile)
                }
            )
        }

        composable<TabDestination.Leaderboard> {
            LeaderboardScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable<TabDestination.EditProfile> {
            EditProfileScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable<TabDestination.Study> {
            StudyScreen()
        }

        composable<TabDestination.Settings> {
            SettingsScreen(
                onNavigateToWordManager = {
                    navController.navigate(TabDestination.WordManager)
                },
                onNavigateToSubscription = {
                    navController.navigate(TabDestination.Subscription)
                }
            )
        }

        composable<TabDestination.WordManager> {
            WordManagerScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable<TabDestination.Subscription> {
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
                    onCancelSubscription = { subscriptionViewModel.cancelSubscription() }
                ),
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}

internal fun NavHostController.navigateToTab(destination: TabDestination) {
    navigate(destination) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
