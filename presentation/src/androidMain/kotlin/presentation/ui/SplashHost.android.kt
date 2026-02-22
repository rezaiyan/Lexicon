package presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import org.koin.compose.viewmodel.koinViewModel
import presentation.feature.auth.AuthViewModel

// Android: native splash handles the visual — just run session verification with no delay.
@Composable
actual fun SplashHost(onEnd: () -> Unit) {
    val authViewModel = koinViewModel<AuthViewModel>()
    LaunchedEffect(Unit) {
        authViewModel.verifyAndRestoreSession(onComplete = onEnd)
    }
}
