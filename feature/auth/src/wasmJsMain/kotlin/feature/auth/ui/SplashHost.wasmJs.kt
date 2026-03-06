package feature.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun SplashHost(onEnd: () -> Unit) {
    LaunchedEffect(Unit) { onEnd() }
}
