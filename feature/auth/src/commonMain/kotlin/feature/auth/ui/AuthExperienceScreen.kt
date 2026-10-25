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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import feature.auth.AuthEffect
import feature.auth.AuthPhase
import androidx.compose.material3.SnackbarHostState
import components.snackbar.LexiconSnackbarHost
import events.OnEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import lexicon.resources.generated.resources.passkey_none_on_device
import lexicon.resources.generated.resources.passkey_removed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import expects.OverrideSystemBars
import expects.openUrl
import lexicon.resources.generated.resources.privacy_policy
import lexicon.resources.generated.resources.terms_of_use
import theme.AppColors
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.app_name
import lexicon.resources.generated.resources.sign_in_failed
import lexicon.resources.generated.resources.sign_in_with_passkey
import lexicon.resources.generated.resources.sign_in_with_passkey_hint

private enum class SignInProvider { GOOGLE, APPLE, PASSKEY }

@Composable
fun AuthExperienceScreen(
    phase: AuthPhase,
    onVerifySession: (onComplete: () -> Unit) -> Unit,
    onSessionVerified: () -> Unit,
    onLoginWithGoogle: suspend (String) -> Unit,
    onLoginWithApple: (String, String?, String) -> Unit,
    isLoading: Boolean = false,
    error: String? = null,
    /** Null hides passkey sign-in (the device can't do passkeys). */
    onLoginWithPasskey: (() -> Unit)? = null,
    effects: Flow<AuthEffect> = emptyFlow(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val noPasskeyText = stringResource(Res.string.passkey_none_on_device)
    val passkeyRemovedText = stringResource(Res.string.passkey_removed)
    OnEvents(effects) { effect ->
        when (effect) {
            AuthEffect.NoPasskeyOnDevice -> snackbarHostState.showSnackbar(noPasskeyText)
            AuthEffect.PasskeyRemoved -> snackbarHostState.showSnackbar(passkeyRemovedText)
        }
    }

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

    // The backdrop runs behind both bars; opaque bars would cut it into off-colour strips
    OverrideSystemBars(
        statusBarColor = Color.Transparent,
        navigationBarColor = Color.Transparent,
        darkIcons = !isDarkBackground(),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LoginBackdrop()

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { LexiconSnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.weight(1f))

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
                        onLoginWithPasskey = onLoginWithPasskey?.let { login ->
                            {
                                activeProvider = SignInProvider.PASSKEY
                                firebaseSignInError = false
                                login()
                            }
                        },
                        onGoogleError = { activeProvider = null; firebaseSignInError = true },
                        onAppleError = { activeProvider = null },
                    )
                }

                Spacer(modifier = Modifier.height(Theme.spacing.xl))
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg),
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    ) {
        val appName = stringResource(Res.string.app_name)
        Box(
            modifier = Modifier
                .size(LogoTileSize)
                .shadow(
                    elevation = Theme.elevation.modal,
                    shape = RoundedCornerShape(LogoTileCorner),
                    spotColor = AppColors.primary,
                )
                .background(AppColors.primary, RoundedCornerShape(LogoTileCorner)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = appName.take(1).uppercase(),
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
        }
        Text(
            text = appName.uppercase(),
            style = TextStyle(
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.5.sp,
                color = if (isDarkBackground()) AppColors.accentLavender else AppColors.brandInk,
            ),
        )
    }
}

@Composable
private fun isDarkBackground(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Soft brand gradient with two blurred light blobs; deep-purple variant in dark theme. */
@Composable
private fun LoginBackdrop() {
    val dark = isDarkBackground()
    val stops = if (dark) {
        listOf(
            MaterialTheme.colorScheme.background,
            AppColors.primary.copy(alpha = 0.18f).compositeOver(MaterialTheme.colorScheme.background),
            AppColors.primary.copy(alpha = 0.35f).compositeOver(MaterialTheme.colorScheme.background),
        )
    } else {
        listOf(AppColors.loginGradientTop, AppColors.loginGradientMiddle, AppColors.loginGradientBottom)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(0f to stops[0], 0.55f to stops[1], 1f to stops[2])
                )
            )
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AppColors.primary.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(size.width * 0.05f, size.height * 0.1f),
                        radius = size.width * 0.5f,
                    ),
                    radius = size.width * 0.5f,
                    center = Offset(size.width * 0.05f, size.height * 0.1f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = if (dark) 0.08f else 0.7f), Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height * 0.65f),
                        radius = size.width * 0.55f,
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width * 0.95f, size.height * 0.65f),
                )
            }
    )
}

private val LogoTileSize = 96.dp
private val LogoTileCorner = 28.dp

@Composable
private fun SignInCard(
    errorMessage: String?,
    isLoading: Boolean,
    activeProvider: SignInProvider?,
    onLoginWithGoogle: suspend (String) -> Unit,
    onLoginWithApple: (String, String?, String) -> Unit,
    onLoginWithPasskey: (() -> Unit)?,
    onGoogleError: () -> Unit,
    onAppleError: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
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

        if (onLoginWithPasskey != null) {
            PasskeySignInButton(
                onClick = onLoginWithPasskey,
                isLoading = isLoading && activeProvider == SignInProvider.PASSKEY,
                enabled = !isLoading,
            )
        }

        LegalLinks()
    }
}

/**
 * Quieter than the Google and Apple buttons: it only works for someone who already made a passkey,
 * which the hint under it says.
 */
@Composable
private fun PasskeySignInButton(onClick: () -> Unit, isLoading: Boolean, enabled: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PasskeyButton(onClick = onClick, isLoading = isLoading, enabled = enabled)
        Text(
            text = stringResource(Res.string.sign_in_with_passkey_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PasskeyButton(onClick: () -> Unit, isLoading: Boolean, enabled: Boolean) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(Theme.dimensions.buttonHeight),
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(imageVector = Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(Theme.spacing.sm))
            Text(text = stringResource(Res.string.sign_in_with_passkey), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun LegalLinks() {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegalLink(text = stringResource(Res.string.terms_of_use), url = TERMS_URL)
        Text(
            text = "·",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LegalLink(text = stringResource(Res.string.privacy_policy), url = PRIVACY_URL)
    }
}

@Composable
private fun LegalLink(text: String, url: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        color = if (isDarkBackground()) AppColors.accentLavender else AppColors.brandInk,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .clickable { openUrl(url) }
            .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xs),
    )
}

private const val TERMS_URL = "https://alirezaiyan.com/vokab/terms"
private const val PRIVACY_URL = "https://alirezaiyan.com/vokab/privacy"
