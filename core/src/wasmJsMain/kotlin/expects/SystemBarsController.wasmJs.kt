package expects

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * WasmJs implementation of SetSystemBarsColor
 * No-op on web platform - browsers manage their own chrome
 */
@Composable
actual fun SetSystemBarsColor(
    statusBarColor: Color,
    navigationBarColor: Color,
    darkIcons: Boolean
) {
    // No-op: web browsers manage their own system bars
}
