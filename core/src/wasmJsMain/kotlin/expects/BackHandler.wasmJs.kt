package expects

import androidx.compose.runtime.Composable

/**
 * WasmJs implementation of BackHandler
 * No-op on web platform - browser handles its own navigation
 */
@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    // No-op: browser handles back navigation natively
}
