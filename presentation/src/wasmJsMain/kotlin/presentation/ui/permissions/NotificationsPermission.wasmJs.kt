package presentation.ui.permissions

import androidx.compose.runtime.Composable

@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit
): () -> Unit {
    return { onResult(false) }
}

@Composable
actual fun wasNotificationPermissionDenied(): Boolean {
    return false
}
