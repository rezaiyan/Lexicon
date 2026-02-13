package presentation.ui.components.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.AppleSignInButton
import presentation.ui.components.GoogleSignInContainer
import theme.Theme
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.offline_usage_info
import vokab.resources.generated.resources.sign_in_description
import vokab.resources.generated.resources.sign_in_to_vokab

private enum class SignInProvider {
    GOOGLE,
    APPLE
}

@Composable
fun AuthenticationSection(
    isLoading: Boolean,
    onLoginWithGoogle: suspend (String) -> Unit,
    onLoginWithApple: (String, String?, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeProvider by remember { mutableStateOf<SignInProvider?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Theme.spacing.sectionSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(Theme.dimensions.iconSizeHuge),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))

        Text(
            text = stringResource(Res.string.sign_in_to_vokab),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(Res.string.sign_in_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Theme.spacing.large))

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

        Spacer(modifier = Modifier.height(48.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(Theme.spacing.cardPadding)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(Res.string.offline_usage_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
