package presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import presentation.ui.components.AppleSignInButton
import presentation.ui.components.GoogleSignInContainer

private enum class SignInProvider {
    GOOGLE,
    APPLE
}

@Composable
fun AuthGateScreen(
    onLoginWithGoogle: suspend (String) -> Unit,
    onLoginWithApple: (String, String?, String) -> Unit,
    onDevLogin: (() -> Unit)? = null,
    isLoading: Boolean = false
) {
    var activeProvider by remember { mutableStateOf<SignInProvider?>(null) }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Lexicon",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sign in to start learning vocabulary",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            GoogleSignInContainer(
                onIdToken = { idToken ->
                    activeProvider = SignInProvider.GOOGLE
                    onLoginWithGoogle(idToken)
                },
                isLoading = isLoading && activeProvider == SignInProvider.GOOGLE,
            )

            AppleSignInButton(
                onSignInSuccess = { idToken, fullName, appleUserId ->
                    activeProvider = SignInProvider.APPLE
                    onLoginWithApple(idToken, fullName, appleUserId)
                },
                onSignInFailure = {
                    activeProvider = null
                },
                isLoading = isLoading && activeProvider == SignInProvider.APPLE,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            // TODO: Remove dev login after testing
            if (onDevLogin != null) {
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onDevLogin) {
                    Text("Dev Login (skip auth)", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
