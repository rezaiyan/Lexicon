package feature.auth.ui

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import auth.awaitGoogleSignIn
import auth.awaitRedirectResult
import kotlinx.coroutines.launch

@Composable
actual fun GoogleSignInContainer(
    onIdToken: suspend (String) -> Unit,
    onError: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // On mobile, sign-in uses a redirect flow. When the page reloads after
    // the redirect, we check for the result here instead of inside the button click.
    LaunchedEffect(Unit) {
        val token = awaitRedirectResult()
        if (token != null) onIdToken(token)
    }

    Button(
        onClick = {
            coroutineScope.launch {
                try {
                    val token = awaitGoogleSignIn()
                    onIdToken(token)
                } catch (e: Exception) {
                    onError()
                }
            }
        },
        enabled = !isLoading,
        modifier = modifier.height(50.dp)
    ) {
        Text(if (isLoading) "Signing in..." else "Sign in with Google")
    }
}
