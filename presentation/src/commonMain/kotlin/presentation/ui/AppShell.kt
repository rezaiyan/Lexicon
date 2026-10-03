package presentation.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import domain.auth.usecase.GetFeatureAccessUseCase
import feature.insights.navigation.InsightsRoute
import feature.profile.navigation.ProfileRoute
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.rememberCoroutineScope
import domain.subscription.usecase.RefreshFeatureAccessUseCase
import kotlinx.coroutines.launch
import presentation.model.SettingsRoute
import presentation.model.TabDestination
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.insights_title
import lexicon.resources.generated.resources.profile
import lexicon.resources.generated.resources.study
import lexicon.resources.generated.resources.words_tab
import theme.Theme

@Composable
internal fun AppContent(
    navController: NavHostController,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val layoutType = currentNavigationSuiteType()

    val getFeatureAccessUseCase = koinInject<GetFeatureAccessUseCase>()
    val featureAccess by remember(getFeatureAccessUseCase) { getFeatureAccessUseCase() }.collectAsState(initial = null)
    val hasPremiumAccess = featureAccess?.userAccess?.hasPremiumAccess == true

    // Pick up grants, expirations and purchases made elsewhere when the app comes back to the
    // foreground. Throttled in the data layer, so frequent resumes don't hit the network.
    val refreshFeatureAccess = koinInject<RefreshFeatureAccessUseCase>()
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(refreshFeatureAccess) {
        scope.launch { refreshFeatureAccess() }
        onPauseOrDispose { }
    }

    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val indicatorColor = selectedColor.copy(alpha = Theme.opacity.focus)
    val itemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = selectedColor,
            selectedTextColor = selectedColor,
            indicatorColor = indicatorColor,
            unselectedIconColor = unselectedColor,
            unselectedTextColor = unselectedColor,
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = selectedColor,
            selectedTextColor = selectedColor,
            indicatorColor = indicatorColor,
            unselectedIconColor = unselectedColor,
            unselectedTextColor = unselectedColor,
        ),
    )

    NavigationSuiteScaffold(
        layoutType = layoutType,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surface,
            navigationRailContainerColor = MaterialTheme.colorScheme.surface,
        ),
        navigationSuiteItems = {
            // Settings is pushed on top of a tab; highlight the tab it was opened from
            val tabDestination = if (currentDestination?.hasRoute<SettingsRoute>() == true) {
                navController.previousBackStackEntry?.destination
            } else {
                currentDestination
            }
            val studySelected = tabDestination?.hasRoute<TabDestination.Study>() == true
            val insightsSelected = tabDestination?.hasRoute<InsightsRoute>() == true
            val wordsSelected = tabDestination?.hasRoute<TabDestination.Words>() == true
            val profileSelected = tabDestination?.hasRoute<ProfileRoute>() == true

            item(
                selected = studySelected,
                onClick = { navController.selectTab(TabDestination.Study, studySelected) },
                icon = { TabIcon(Icons.Outlined.School) },
                label = { TabLabel(stringResource(Res.string.study), studySelected) },
                colors = itemColors,
            )
            item(
                selected = wordsSelected,
                onClick = { navController.selectTab(TabDestination.Words, wordsSelected) },
                icon = { TabIcon(Icons.AutoMirrored.Outlined.List) },
                label = { TabLabel(stringResource(Res.string.words_tab), wordsSelected) },
                colors = itemColors,
            )
            if (hasPremiumAccess) {
                item(
                    selected = insightsSelected,
                    onClick = { navController.selectTab(InsightsRoute, insightsSelected) },
                    icon = { TabIcon(Icons.Outlined.BarChart) },
                    label = { TabLabel(stringResource(Res.string.insights_title), insightsSelected) },
                    colors = itemColors,
                )
            }
            item(
                selected = profileSelected,
                onClick = { navController.selectTab(ProfileRoute, profileSelected) },
                icon = { TabIcon(Icons.Outlined.Person) },
                label = { TabLabel(stringResource(Res.string.profile), profileSelected) },
                colors = itemColors,
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = if (layoutType != NavigationSuiteType.NavigationBar) {
                WindowInsets.navigationBars
            } else {
                WindowInsets(0)
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = if (layoutType != NavigationSuiteType.NavigationBar) {
                        Modifier.navigationBarsPadding()
                    } else {
                        Modifier
                    },
                    snackbar = { snackbarData ->
                        Snackbar(
                            snackbarData = snackbarData,
                            modifier = Modifier.padding(start = Theme.spacing.md, end = Theme.spacing.md),
                            containerColor = if (snackbarData.visuals.message.startsWith("[Error]"))
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (snackbarData.visuals.message.startsWith("[Error]"))
                                MaterialTheme.colorScheme.onErrorContainer
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                )
            }
        ) { innerPadding ->
            NavigationGraph(
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
                navController = navController,
            )
        }
    }
}

@Composable
private fun TabIcon(icon: ImageVector) {
    // Label already names the tab
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(Theme.dimensions.iconSize),
    )
}

@Composable
private fun TabLabel(text: String, selected: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
    )
}
