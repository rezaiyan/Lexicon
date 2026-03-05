@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)

package presentation.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import org.jetbrains.compose.resources.stringResource
import presentation.model.TabDestination
import presentation.ui.components.AnimatedNavIcon
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.profile
import lexicon.resources.generated.resources.settings
import lexicon.resources.generated.resources.study
import theme.Theme

@Composable
internal fun AppContent(
    navController: NavHostController,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val layoutType = currentNavigationSuiteType()

    NavigationSuiteScaffold(
        layoutType = layoutType,
        navigationSuiteItems = {
            val profileSelected = currentDestination?.hasRoute<TabDestination.Profile>() == true
            val studySelected = currentDestination?.hasRoute<TabDestination.Study>() == true
            val settingsSelected = currentDestination?.hasRoute<TabDestination.Settings>() == true

            item(
                selected = profileSelected,
                onClick = { navController.navigateToTab(TabDestination.Profile) },
                icon = {
                    AnimatedNavIcon(
                        icon = Icons.Filled.Person,
                        contentDescription = stringResource(Res.string.profile),
                        selected = profileSelected
                    )
                },
                label = { Text(stringResource(Res.string.profile)) }
            )
            item(
                selected = studySelected,
                onClick = { navController.navigateToTab(TabDestination.Study) },
                icon = {
                    AnimatedNavIcon(
                        icon = Icons.Filled.Book,
                        contentDescription = stringResource(Res.string.study),
                        selected = studySelected
                    )
                },
                label = { Text(stringResource(Res.string.study)) }
            )
            item(
                selected = settingsSelected,
                onClick = { navController.navigateToTab(TabDestination.Settings) },
                icon = {
                    AnimatedNavIcon(
                        icon = Icons.Filled.Settings,
                        contentDescription = stringResource(Res.string.settings),
                        selected = settingsSelected
                    )
                },
                label = { Text(stringResource(Res.string.settings)) }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
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
