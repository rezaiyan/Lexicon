package presentation.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import auth.awaitGoogleSignIn
import kotlinx.coroutines.launch

@Composable
actual fun GoogleSignInContainer(
    onIdToken: suspend (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    Button(
        onClick = {
            coroutineScope.launch {
                val token = awaitGoogleSignIn()
                onIdToken(token)
            }
        },
        enabled = !isLoading,
        modifier = modifier.height(50.dp)
    ) {
        Text(if (isLoading) "Signing in..." else "Sign in with Google")
    }
}
