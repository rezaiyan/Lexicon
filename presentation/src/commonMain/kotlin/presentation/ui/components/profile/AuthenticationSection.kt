package presentation.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmk.kmpauth.firebase.google.GoogleButtonUiContainerFirebase
import com.mmk.kmpauth.uihelper.google.GoogleButtonMode
import com.mmk.kmpauth.uihelper.google.GoogleSignInButton
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import presentation.feature.profile.ProfileViewModel
import presentation.ui.components.AppleSignInButton
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
    profileViewModel: ProfileViewModel,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var activeProvider by remember { mutableStateOf<SignInProvider?>(null) }
    val coroutineScope = rememberCoroutineScope()

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

        val isDarkTheme = isSystemInDarkTheme()
        val googleButtonStyle = if (isDarkTheme) GoogleButtonMode.Light else GoogleButtonMode.Dark

        GoogleButtonUiContainerFirebase(
            onResult = { result ->
                activeProvider = SignInProvider.GOOGLE
                result.onSuccess { firebaseUser ->
                    if (firebaseUser != null) {
                        coroutineScope.launch {
                            try {
                                val idToken = firebaseUser.getIdToken(false)
                                if (idToken != null) {
                                    profileViewModel.loginWithGoogle(idToken)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                GoogleSignInButton(
                    modifier = Modifier
                        .height(50.dp)
                        .fillMaxWidth(),
                    fontSize = 18.sp,
                    mode = googleButtonStyle
                ) {
                    activeProvider = SignInProvider.GOOGLE
                    this@GoogleButtonUiContainerFirebase.onClick()
                }

                if (isLoading && activeProvider == SignInProvider.GOOGLE) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }

        AppleSignInButton(
            onSignInSuccess = { idToken, fullName, appleUserId ->
                activeProvider = SignInProvider.APPLE
                profileViewModel.loginWithApple(idToken, fullName, appleUserId)
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

