package presentation.ui

import androidx.compose.runtime.Composable
import presentation.ui.screens.SplashScreen

// iOS: show the full animated Compose splash screen.
@Composable
actual fun SplashHost(onEnd: () -> Unit) {
    SplashScreen(onEnd = onEnd)
}
