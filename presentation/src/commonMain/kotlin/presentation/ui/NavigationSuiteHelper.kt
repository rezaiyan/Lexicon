package presentation.ui

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable

@Composable
expect fun currentNavigationSuiteType(): NavigationSuiteType
