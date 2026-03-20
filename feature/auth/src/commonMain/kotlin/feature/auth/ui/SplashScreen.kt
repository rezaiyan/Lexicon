package feature.auth.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import components.animation.LottieGradientBackground
import feature.auth.AuthViewModel
import theme.AppColors
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.app_name

// Process-level flag — survives ViewModel recreation but resets on full process restart.
// Prevents the splash animation from showing again after login triggers a ViewModel re-init.
private var splashShownThisSession = false

@Composable
fun SplashScreen(
    onEnd: () -> Unit,
) {
    val authViewModel = koinInject<AuthViewModel>()

    var startAnimation by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(500)
    )

    LaunchedEffect(Unit) {
        if (splashShownThisSession) {
            authViewModel.verifyAndRestoreSession(onComplete = onEnd)
            return@LaunchedEffect
        }
        splashShownThisSession = true
        startAnimation = true
        val sessionReady = CompletableDeferred<Unit>()
        authViewModel.verifyAndRestoreSession(onComplete = { sessionReady.complete(Unit) })
        delay(1800)
        sessionReady.await()
        onEnd()
    }

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
            ),
        contentAlignment = Alignment.Center
    ) {
        LottieGradientBackground(
            modifier = Modifier.fillMaxSize(),
            alpha = 0.55f,
            tint = AppColors.primary.copy(alpha = 0.18f),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
        ) {
            val nameGradient = remember {
                Brush.linearGradient(listOf(AppColors.primary, AppColors.accentLavender))
            }
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
}
