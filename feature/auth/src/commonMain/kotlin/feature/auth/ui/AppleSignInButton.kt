package feature.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun AppleSignInButton(
    onSignInSuccess: (idToken: String, fullName: String?, appleUserId: String) -> Unit,
    onSignInFailure: (error: String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
)
