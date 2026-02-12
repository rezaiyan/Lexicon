package presentation.ui.permissions

import androidx.compose.runtime.Composable

@Composable
expect fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit
): () -> Unit

@Composable
expect fun wasNotificationPermissionDenied(): Boolean


