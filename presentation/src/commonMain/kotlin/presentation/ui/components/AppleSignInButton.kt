package presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific Apple Sign In button
 * Shows on iOS, hidden on other platforms
 */
@Composable
expect fun AppleSignInButton(
    onSignInSuccess: (idToken: String, fullName: String?, appleUserId: String) -> Unit,
    onSignInFailure: (error: String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
)

