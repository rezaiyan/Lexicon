package presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun AppleSignInButton(
    onSignInSuccess: (idToken: String, fullName: String?, appleUserId: String) -> Unit,
    onSignInFailure: (error: String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier
) {
}