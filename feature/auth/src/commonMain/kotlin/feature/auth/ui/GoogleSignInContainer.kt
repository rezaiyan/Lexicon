package feature.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun GoogleSignInContainer(
    onIdToken: suspend (String) -> Unit,
    onError: () -> Unit = {},
    isLoading: Boolean,
    modifier: Modifier = Modifier
)
