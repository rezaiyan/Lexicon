package expects

import androidx.compose.runtime.Composable

/**
 * Platform-agnostic back handler
 * Handles system back button on Android and swipe-back gesture on iOS
 */
@Composable
expect fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
)

