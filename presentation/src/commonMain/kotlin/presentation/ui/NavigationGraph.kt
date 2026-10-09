package presentation.ui

import analytics.IAnalyticsTracker
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import events.OnEvents
import feature.insights.navigation.insightsGraph
import feature.leaderboard.navigation.showLeaderboard
import feature.profile.navigation.profileGraph
import feature.subscription.navigation.SubscriptionRoute
import feature.subscription.navigation.subscriptionGraph
import org.koin.compose.koinInject
import overlay.LocalOverlayHost
import presentation.model.SettingsRoute
import presentation.model.TabDestination
import presentation.navigation.NotificationDestination
import presentation.navigation.NotificationNavigator
import presentation.ui.components.settings.showNotificationSettingsSheet
import presentation.ui.screens.SettingsScreen
import presentation.ui.screens.StudyScreen
import presentation.ui.screens.settings.WordManagerScreen

@Composable
internal fun NavigationGraph(
    modifier: Modifier,
    navController: NavHostController,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val overlayHost = LocalOverlayHost.current
    val analyticsTracker = koinInject<IAnalyticsTracker>()

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val screenName = destination.route
                ?.substringAfterLast('.')
                ?.substringBefore('/')
                ?: "unknown"
            analyticsTracker.logScreenView(screenName)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    // Taps on actionable notifications (e.g. a failed renewal, a review reminder) open the screen they're about
    OnEvents(koinInject<NotificationNavigator>().destinations) { destination ->
        when (destination) {
            NotificationDestination.Subscription -> navController.navigate(SubscriptionRoute) {
                launchSingleTop = true
            }
            NotificationDestination.Study -> navController.navigateToTab(TabDestination.Study)
        }
    }

    NavHost(
        navController = navController,
        startDestination = TabDestination.Study,
        modifier = modifier.fillMaxSize(),
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        // Feature-owned subgraphs
        profileGraph(
            snackbarHostState = snackbarHostState,
            onOpenSubscription = { navController.navigate(SubscriptionRoute) },
        )

        subscriptionGraph(
            snackbarHostState = snackbarHostState,
            onNavigateBack = { navController.navigateUp() },
        )

        insightsGraph(
            onShowLeaderboard = { overlayHost.showLeaderboard() },
            onNavigateToNotificationSettings = { overlayHost.showNotificationSettingsSheet() },
        )

        // Presentation-owned routes (screens still in :presentation)
        composable<TabDestination.Study> {
            StudyScreen(
                onNavigateToSettings = { navController.navigate(SettingsRoute) },
                onOpenSubscription = { navController.navigate(SubscriptionRoute) { launchSingleTop = true } },
            )
        }

        composable<TabDestination.Words> {
            WordManagerScreen()
        }

        composable<SettingsRoute> {
            SettingsScreen(onNavigateBack = { navController.navigateUp() })
        }
    }
}

/** Reselecting the active tab pops back to its root; otherwise switches tabs preserving their stacks. */
internal fun NavHostController.selectTab(destination: Any, isSelected: Boolean) {
    if (isSelected) {
        popBackStack(destination, inclusive = false)
    } else {
        navigateToTab(destination)
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
