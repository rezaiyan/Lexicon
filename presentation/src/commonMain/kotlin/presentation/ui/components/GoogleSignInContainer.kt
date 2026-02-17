package presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific Google Sign-In container.
 * On mobile, uses KMPAuth + Firebase. On web, uses Firebase JS SDK.
 */
@Composable
expect fun GoogleSignInContainer(
    onIdToken: suspend (String) -> Unit,
    onError: () -> Unit = {},
    isLoading: Boolean,
    modifier: Modifier = Modifier
)
