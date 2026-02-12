package expects

import androidx.compose.runtime.Composable

/**
 * WasmJs implementation of isSystemInDarkTheme
 * Returns false as a safe default - CSS media query detection requires JS interop
 */
@Composable
actual fun isSystemInDarkTheme(): Boolean = false
