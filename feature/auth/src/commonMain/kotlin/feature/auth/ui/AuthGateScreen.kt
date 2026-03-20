package feature.auth.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.animation.LottieGradientBackground
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.app_name
import lexicon.resources.generated.resources.sign_in_failed
import lexicon.resources.generated.resources.signing_in

private enum class SignInProvider { GOOGLE, APPLE }

@Composable
fun AuthGateScreen(
    onLoginWithGoogle: suspend (String) -> Unit,
    onLoginWithApple: (String, String?, String) -> Unit,
    isLoading: Boolean = false,
    error: String? = null
) {
    var activeProvider by remember { mutableStateOf<SignInProvider?>(null) }
    var firebaseSignInError by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }

    val signInFailedText = stringResource(Res.string.sign_in_failed)
    val errorMessage = error ?: if (firebaseSignInError) signInFailedText else null

    LaunchedEffect(Unit) {
        delay(80)
        started = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()
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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1.5f))

                LogoSection(started = started)

                Spacer(modifier = Modifier.weight(1f))

                SignInCard(
                    started = started,
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
                    onAppleError = { activeProvider = null }
                )

                Spacer(modifier = Modifier.height(Theme.spacing.xxxl))
            }
        }
    }
}

@Composable
private fun AnimatedBackground() {
    val background = MaterialTheme.colorScheme.background
    val primary = AppColors.primary
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to background,
                        0.60f to background,
                        1f to primary.copy(alpha = 0.10f)
                    )
                )
            )
    )
}

@Composable
private fun LogoSection(started: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (started) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    val alpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(500)
    )
    val appName = stringResource(Res.string.app_name)

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
            text = appName,
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
    started: Boolean,
    errorMessage: String?,
    isLoading: Boolean,
    activeProvider: SignInProvider?,
    onLoginWithGoogle: suspend (String) -> Unit,
    onLoginWithApple: (String, String?, String) -> Unit,
    onGoogleError: () -> Unit,
    onAppleError: () -> Unit,
) {
    val cardSlide by animateDpAsState(
        targetValue = if (started) 0.dp else 80.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(450, delayMillis = 200)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.xl)
            .offset(y = cardSlide)
            .alpha(cardAlpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)
    ) {
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        GoogleSignInContainer(
            onIdToken = onLoginWithGoogle,
            onError = onGoogleError,
            isLoading = isLoading && activeProvider == SignInProvider.GOOGLE,
            modifier = Modifier.fillMaxWidth()
        )

        AppleSignInButton(
            onSignInSuccess = onLoginWithApple,
            onSignInFailure = { onAppleError() },
            isLoading = isLoading && activeProvider == SignInProvider.APPLE,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SigningInOverlay() {
    val pulse = rememberInfiniteTransition()
    val glow by pulse.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse
        )
    )
    val textAlpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse
        )
    )

    val appName = stringResource(Res.string.app_name)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.93f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(Theme.spacing.xxxl)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = appName,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    color = AppColors.primary.copy(alpha = glow),
                    modifier = Modifier.graphicsLayer { scaleX = 1.15f; scaleY = 1.15f }
                )
                Text(
                    text = appName,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    color = AppColors.primary,
                )
            }

            Spacer(modifier = Modifier.height(Theme.spacing.lg))

            Text(
                text = stringResource(Res.string.signing_in),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(textAlpha)
            )
        }
    }
}
