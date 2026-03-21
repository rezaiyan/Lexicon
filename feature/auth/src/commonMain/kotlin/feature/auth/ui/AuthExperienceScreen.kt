package feature.auth.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import components.animation.LottieGradientBackground
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import feature.auth.AuthPhase
import theme.AppColors
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.app_name
import lexicon.resources.generated.resources.sign_in_failed

private enum class SignInProvider { GOOGLE, APPLE }

@Composable
fun AuthExperienceScreen(
    phase: AuthPhase,
    onVerifySession: (onComplete: () -> Unit) -> Unit,
    onSessionVerified: () -> Unit,
    onLoginWithGoogle: suspend (String) -> Unit,
    onLoginWithApple: (String, String?, String) -> Unit,
    isLoading: Boolean = false,
    error: String? = null,
) {
    var activeProvider by remember { mutableStateOf<SignInProvider?>(null) }
    var firebaseSignInError by remember { mutableStateOf(false) }
    var logoVisible by remember { mutableStateOf(false) }

    val signInFailedText = stringResource(Res.string.sign_in_failed)
    val errorMessage = error ?: if (firebaseSignInError) signInFailedText else null

    LaunchedEffect(Unit) {
        logoVisible = true
        if (phase == AuthPhase.Verifying) {
            val sessionReady = CompletableDeferred<Unit>()
            onVerifySession { sessionReady.complete(Unit) }
            delay(minimumSplashMs)
            sessionReady.await()
            onSessionVerified()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to MaterialTheme.colorScheme.background,
                            0.60f to MaterialTheme.colorScheme.background,
                            1f to AppColors.primary.copy(alpha = 0.10f)
                        )
                    )
                )
        )

        // Single Lottie instance — persists across Verifying → LoginRequired without restarting
        LottieGradientBackground(
            modifier = Modifier.fillMaxSize(),
            alpha = 0.55f,
            tint = AppColors.primary.copy(alpha = 0.18f),
        )

        Scaffold(containerColor = Color.Transparent) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.weight(1.5f))

                LogoSection(visible = logoVisible)

                Spacer(modifier = Modifier.weight(1f))

                AnimatedVisibility(
                    visible = phase == AuthPhase.LoginRequired,
                    enter = slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        initialOffsetY = { it / 2 },
                    ) + fadeIn(tween(450)),
                    exit = fadeOut(tween(200)),
                ) {
                    SignInCard(
                        errorMessage = errorMessage,
                        isLoading = isLoading,
                        activeProvider = activeProvider,
                        onLoginWithGoogle = { idToken ->
                            activeProvider = SignInProvider.GOOGLE
                            firebaseSignInError = false
                            onLoginWithGoogle(idToken)
                        },
                        onLoginWithApple = { idToken, fullName, userId ->
                            activeProvider = SignInProvider.APPLE
                            firebaseSignInError = false
                            onLoginWithApple(idToken, fullName, userId)
                        },
                        onGoogleError = { activeProvider = null; firebaseSignInError = true },
                        onAppleError = { activeProvider = null },
                    )
                }

                Spacer(modifier = Modifier.height(Theme.spacing.xxxl))
            }
        }
    }
}

@Composable
private fun LogoSection(visible: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        )
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500)
    )
    val nameGradient = remember {
        Brush.linearGradient(listOf(AppColors.primary, AppColors.accentLavender))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    ) {
        Text(
            text = stringResource(Res.string.app_name),
            style = TextStyle(
                brush = nameGradient,
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp,
            ),
        )
    }
}

@Composable
private fun SignInCard(
    errorMessage: String?,
    isLoading: Boolean,
    activeProvider: SignInProvider?,
    onLoginWithGoogle: suspend (String) -> Unit,
    onLoginWithApple: (String, String?, String) -> Unit,
    onGoogleError: () -> Unit,
    onAppleError: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        GoogleSignInContainer(
            onIdToken = onLoginWithGoogle,
            onError = onGoogleError,
            isLoading = isLoading && activeProvider == SignInProvider.GOOGLE,
            modifier = Modifier.fillMaxWidth(),
        )

        AppleSignInButton(
            onSignInSuccess = onLoginWithApple,
            onSignInFailure = { onAppleError() },
            isLoading = isLoading && activeProvider == SignInProvider.APPLE,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
