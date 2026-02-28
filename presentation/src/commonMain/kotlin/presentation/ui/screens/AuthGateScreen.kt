package presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.AppleSignInButton
import presentation.ui.components.GoogleSignInContainer
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.login_required_title
import lexicon.resources.generated.resources.login_required_subtitle
import lexicon.resources.generated.resources.sign_in_failed

private enum class SignInProvider {
    GOOGLE,
    APPLE
}

@Composable
fun AuthGateScreen(
    onLoginWithGoogle: suspend (String) -> Unit,
    onLoginWithApple: (String, String?, String) -> Unit,
    isLoading: Boolean = false,
    error: String? = null
) {
    var activeProvider by remember { mutableStateOf<SignInProvider?>(null) }
    var firebaseSignInError by remember { mutableStateOf(false) }

    val signInFailedText = stringResource(Res.string.sign_in_failed)
    val errorMessage = error ?: if (firebaseSignInError) signInFailedText else null

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Theme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(Res.string.login_required_title),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Theme.spacing.xs))

            Text(
                text = stringResource(Res.string.login_required_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Theme.spacing.xxxl))

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Theme.spacing.sm))
            }

            GoogleSignInContainer(
                onIdToken = { idToken ->
                    activeProvider = SignInProvider.GOOGLE
                    firebaseSignInError = false
                    onLoginWithGoogle(idToken)
                },
                onError = {
                    activeProvider = null
                    firebaseSignInError = true
                },
                isLoading = isLoading && activeProvider == SignInProvider.GOOGLE,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = Theme.dimensions.contentMaxWidth)
            )

            AppleSignInButton(
                onSignInSuccess = { idToken, fullName, appleUserId ->
                    activeProvider = SignInProvider.APPLE
                    firebaseSignInError = false
                    onLoginWithApple(idToken, fullName, appleUserId)
                },
                onSignInFailure = {
                    activeProvider = null
                },
                isLoading = isLoading && activeProvider == SignInProvider.APPLE,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = Theme.dimensions.contentMaxWidth)
                    .padding(top = Theme.spacing.sm)
            )
        }
    }
}
