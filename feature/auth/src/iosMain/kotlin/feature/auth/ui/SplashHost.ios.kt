package feature.auth.ui

import androidx.compose.runtime.Composable

// iOS: show the full animated Compose splash screen.
@Composable
actual fun SplashHost(onEnd: () -> Unit) {
    SplashScreen(onEnd = onEnd)
}
