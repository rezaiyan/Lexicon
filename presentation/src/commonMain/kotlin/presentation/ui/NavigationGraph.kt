package presentation.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import feature.leaderboard.navigation.LeaderboardRoute
import feature.leaderboard.navigation.leaderboardGraph
import feature.profile.navigation.EditProfileRoute
import feature.profile.navigation.ProfileRoute
import feature.profile.navigation.profileGraph
import feature.subscription.navigation.SubscriptionRoute
import feature.subscription.navigation.subscriptionGraph
import presentation.model.TabDestination
import presentation.ui.screens.SettingsScreen
import presentation.ui.screens.StudyScreen
import presentation.ui.screens.settings.WordManagerScreen

@Composable
internal fun NavigationGraph(
    modifier: Modifier,
    navController: NavHostController,
) {
    val snackbarHostState = LocalSnackbarHostState.current

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
        // Feature-owned subgraphs
        profileGraph(
            snackbarHostState = snackbarHostState,
            onNavigateToLeaderboard = { navController.navigate(LeaderboardRoute) },
            onNavigateToEditProfile = { navController.navigate(EditProfileRoute) },
            onNavigateBack = { navController.navigateUp() },
        )

        leaderboardGraph(
            onNavigateBack = { navController.navigateUp() },
        )

        subscriptionGraph(
            snackbarHostState = snackbarHostState,
            onNavigateBack = { navController.navigateUp() },
        )

        // Presentation-owned routes (screens still in :presentation)
        composable<TabDestination.Study> {
            StudyScreen()
        }

        composable<TabDestination.Settings> {
            SettingsScreen(
                onNavigateToWordManager = {
                    navController.navigate(TabDestination.WordManager)
                },
                onNavigateToSubscription = {
                    navController.navigate(SubscriptionRoute)
                }
            )
        }

        composable<TabDestination.WordManager> {
            WordManagerScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}

internal fun NavHostController.navigateToTab(destination: Any) {
    navigate(destination) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
