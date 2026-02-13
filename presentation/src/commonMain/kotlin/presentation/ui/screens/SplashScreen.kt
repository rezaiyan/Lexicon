package presentation.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import presentation.feature.auth.AuthViewModel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.app_name
import lexicon.resources.generated.resources.app_tagline

/**
 * Splash Screen - Self-contained
 * Manages its own ViewModel and session verification internally
 * 
 * Only requires navigation callbacks from parent
 */
@Composable
fun SplashScreen(
    onEnd: () -> Unit,
) {
    val authViewModel = koinInject<AuthViewModel>()
    
    var startAnimation by remember { mutableStateOf(false) }

    // Logo scale animation
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    // Logo alpha animation
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800)
    )

    // Glow pulse animation
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Verify session on splash screen
    LaunchedEffect(Unit) {
        startAnimation = true
        authViewModel.verifyAndRestoreSession(onComplete = onEnd)
        delay(500) // Brief delay for animation to start
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo/Name with neon effect
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Glow effect layer
                Text(
                    text = stringResource(Res.string.app_name),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                    modifier = Modifier
                        .scale(logoScale * 1.1f)
                        .alpha(logoAlpha * 0.5f)
                )

                // Main text
                Text(
                    text = stringResource(Res.string.app_name),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .scale(logoScale)
                        .alpha(logoAlpha)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.app_tagline),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.alpha(logoAlpha)
            )
        }
    }
}

