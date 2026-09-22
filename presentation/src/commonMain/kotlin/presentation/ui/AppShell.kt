package presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import components.GlassBottomNavBar
import components.GlassNavItem
import components.glassBackdropSource
import components.isLiquidGlassSupported
import components.rememberGlassBackdropLayer
import navigation.NativeTabBarBridge
import navigation.isNativeTabBarSupported
import org.jetbrains.compose.resources.stringResource
import presentation.model.TabDestination
import presentation.ui.components.AnimatedNavIcon
import lexicon.resources.generated.resources.Res
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
    val studySelected = currentDestination?.hasRoute<TabDestination.Study>() == true
    val settingsSelected = currentDestination?.hasRoute<TabDestination.Settings>() == true

    // Only the phone bottom-bar layout gets special tab-bar treatment; rail/drawer layouts
    // (tablet, desktop) always keep the standard NavigationSuiteScaffold bar.
    when {
        layoutType == NavigationSuiteType.NavigationBar && isNativeTabBarSupported() -> {
            // iOS 26+: a native SwiftUI TabView owns the bar (genuine OS Liquid Glass).
            // Compose renders content only and syncs selection with NativeTabBarBridge.
            NativeTabBarContentShell(
                navController = navController,
                studySelected = studySelected,
                settingsSelected = settingsSelected,
                snackbarHostState = snackbarHostState,
            )
        }
        // Liquid glass needs RenderEffect/RuntimeShader support that not every OS version has.
        layoutType == NavigationSuiteType.NavigationBar && isLiquidGlassSupported() -> {
            GlassBottomBarShell(
                navController = navController,
                studySelected = studySelected,
                settingsSelected = settingsSelected,
                snackbarHostState = snackbarHostState,
            )
        }
        else -> {
            LegacyShell(
                navController = navController,
                layoutType = layoutType,
                studySelected = studySelected,
                settingsSelected = settingsSelected,
                snackbarHostState = snackbarHostState,
            )
        }
    }
}

@Composable
private fun NativeTabBarContentShell(
    navController: NavHostController,
    studySelected: Boolean,
    settingsSelected: Boolean,
    snackbarHostState: SnackbarHostState,
) {
    DisposableEffect(navController) {
        NativeTabBarBridge.setTabTapListener { tab ->
            val destination = if (tab == "settings") TabDestination.Settings else TabDestination.Study
            navController.navigateToTab(destination)
        }
        onDispose { NativeTabBarBridge.setTabTapListener {} }
    }

    LaunchedEffect(studySelected, settingsSelected) {
        when {
            studySelected -> NativeTabBarBridge.reportCurrentTab("study")
            settingsSelected -> NativeTabBarBridge.reportCurrentTab("settings")
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData -> LexiconSnackbar(snackbarData) }
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

@Composable
private fun GlassBottomBarShell(
    navController: NavHostController,
    studySelected: Boolean,
    settingsSelected: Boolean,
    snackbarHostState: SnackbarHostState,
) {
    Box(Modifier.fillMaxSize()) {
        val backdropLayer = rememberGlassBackdropLayer()

        Scaffold(
            modifier = Modifier.glassBackdropSource(backdropLayer),
            contentWindowInsets = WindowInsets(0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.navigationBarsPadding(),
                    snackbar = { snackbarData -> LexiconSnackbar(snackbarData) }
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

        GlassBottomNavBar(
            backdropLayer = backdropLayer,
            items = listOf(
                GlassNavItem(
                    selected = studySelected,
                    label = stringResource(Res.string.study),
                    onClick = { navController.navigateToTab(TabDestination.Study) },
                    icon = {
                        AnimatedNavIcon(
                            icon = Icons.Filled.Book,
                            contentDescription = stringResource(Res.string.study),
                            selected = studySelected
                        )
                    }
                ),
                GlassNavItem(
                    selected = settingsSelected,
                    label = stringResource(Res.string.settings),
                    onClick = { navController.navigateToTab(TabDestination.Settings) },
                    icon = {
                        AnimatedNavIcon(
                            icon = Icons.Filled.Settings,
                            contentDescription = stringResource(Res.string.settings),
                            selected = settingsSelected
                        )
                    }
                ),
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun LegacyShell(
    navController: NavHostController,
    layoutType: NavigationSuiteType,
    studySelected: Boolean,
    settingsSelected: Boolean,
    snackbarHostState: SnackbarHostState,
) {
    NavigationSuiteScaffold(
        layoutType = layoutType,
        navigationSuiteItems = {
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
                    snackbar = { snackbarData -> LexiconSnackbar(snackbarData) }
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
private fun LexiconSnackbar(snackbarData: SnackbarData) {
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
