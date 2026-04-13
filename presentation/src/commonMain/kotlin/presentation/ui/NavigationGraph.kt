package presentation.ui

import analytics.IAnalyticsTracker
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import feature.insights.navigation.insightsGraph
import feature.leaderboard.navigation.showLeaderboard
import feature.profile.navigation.profileGraph
import feature.settings.SettingsViewModel
import feature.subscription.navigation.SubscriptionRoute
import feature.subscription.navigation.subscriptionGraph
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import overlay.LocalOverlayHost
import overlay.bottomsheet.showSizeToFitBottomSheet
import presentation.model.TabDestination
import presentation.ui.components.NotificationPermissionContent
import presentation.ui.components.NotificationSettingsContent
import presentation.ui.permissions.rememberNotificationPermissionRequester
import presentation.ui.permissions.wasNotificationPermissionDenied
import presentation.ui.screens.SettingsScreen
import presentation.ui.screens.StudyScreen

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
        )

        subscriptionGraph(
            snackbarHostState = snackbarHostState,
            onNavigateBack = { navController.navigateUp() },
        )

        insightsGraph(
            onNavigateBack = { navController.navigateUp() },
            onShowLeaderboard = { overlayHost.showLeaderboard() },
            snackbarHostState = snackbarHostState,
            onNavigateToNotificationSettings = {
                overlayHost.showSizeToFitBottomSheet(tag = "notification-settings") { nav ->
                    val settingsViewModel = koinViewModel<SettingsViewModel>()
                    val currentState by settingsViewModel.state()
                    if (currentState.screen.systemNotificationsEnabled) {
                        NotificationSettingsContent(
                            notificationsEnabled = currentState.screen.notificationsEnabled,
                            systemNotificationsEnabled = currentState.screen.systemNotificationsEnabled,
                            reviewRemindersEnabled = currentState.screen.reviewRemindersEnabled,
                            onNotificationsToggle = { settingsViewModel.setNotificationsEnabled(it) },
                            onReviewRemindersToggle = { settingsViewModel.setReviewRemindersEnabled(it) },
                            onDismiss = { nav.dismiss() }
                        )
                    } else {
                        val deniedPreviously = wasNotificationPermissionDenied()
                        val requestPermission = rememberNotificationPermissionRequester { granted ->
                            if (granted) settingsViewModel.setNotificationsEnabled(true)
                            settingsViewModel.refreshNotificationPermissionStatus()
                            nav.dismiss()
                        }
                        NotificationPermissionContent(
                            onDismiss = { nav.dismiss() },
                            onEnableNotifications = {
                                if (deniedPreviously) {
                                    nav.dismiss()
                                    settingsViewModel.requestNotificationPermission()
                                    settingsViewModel.refreshNotificationPermissionStatus()
                                } else {
                                    requestPermission()
                                }
                            }
                        )
                    }
                }
            }
        )

        // Presentation-owned routes (screens still in :presentation)
        composable<TabDestination.Study> {
            StudyScreen(
            )
        }

        composable<TabDestination.Settings> {
            SettingsScreen(
                onNavigateToSubscription = {
                    navController.navigate(SubscriptionRoute)
                }
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
