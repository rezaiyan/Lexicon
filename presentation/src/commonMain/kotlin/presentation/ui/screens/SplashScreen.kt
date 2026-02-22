package presentation.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.ai_book
import lexicon.resources.generated.resources.app_name
import lexicon.resources.generated.resources.app_tagline
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import presentation.feature.auth.AuthViewModel

/**
 * Splash Screen with font motion and animated logo.
 *
 * Animation sequence (plays to completion before navigating):
 *   Phase 1 (0ms)      – Logo icon springs in with scale + rotation bounce
 *   Phase 2 (200ms)    – Letters rise from below with 75 ms stagger
 *   Phase 3 (~1 000ms) – Tagline slides up and fades in
 *
 * Navigation fires only when BOTH animations and session verification finish.
 */
@Composable
fun SplashScreen(onEnd: () -> Unit) {
    val authViewModel = koinInject<AuthViewModel>()
    val appName = stringResource(Res.string.app_name)
    val density = LocalDensity.current

    val letterDropPx = with(density) { 50.dp.toPx() }
    val taglineDropPx = with(density) { 16.dp.toPx() }

    // ── Icon animations ──────────────────────────────────────────────────────
    val iconScale = remember { Animatable(0.3f) }
    val iconAlpha = remember { Animatable(0f) }
    val iconRotation = remember { Animatable(-10f) }

    // ── Per-letter animations ────────────────────────────────────────────────
    val letterCount = appName.length
    val letterOffsets = remember(letterCount) { List(letterCount) { Animatable(letterDropPx) } }
    val letterAlphas = remember(letterCount) { List(letterCount) { Animatable(0f) } }

    // ── Tagline animations ───────────────────────────────────────────────────
    val taglineAlpha = remember { Animatable(0f) }
    val taglineOffset = remember { Animatable(taglineDropPx) }

    // ── Infinite glow pulse ──────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    LaunchedEffect(Unit) {
        // Bridge the callback-based session verification into a Deferred so we
        // can await it alongside the animation sequence.
        val sessionDeferred = CompletableDeferred<Unit>()
        authViewModel.verifyAndRestoreSession(onComplete = {
            sessionDeferred.complete(Unit)
        })

        // ── Phase 1: icon entrance (runs concurrently with phase 2 start) ──
        launch { iconAlpha.animateTo(1f, tween(durationMillis = 500)) }
        launch {
            iconScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            iconRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        // ── Phase 2: staggered letter rise ───────────────────────────────────
        delay(200)
        val letterJobs = appName.indices.map { i ->
            launch {
                delay(75L * i)
                launch { letterAlphas[i].animateTo(1f, tween(durationMillis = 200)) }
                letterOffsets[i].animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
        letterJobs.forEach { it.join() }

        // ── Phase 3: tagline slide-up ─────────────────────────────────────────
        delay(200)
        val taglineAlphaJob = launch { taglineAlpha.animateTo(1f, tween(durationMillis = 400)) }
        val taglineOffsetJob = launch {
            taglineOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
        taglineAlphaJob.join()
        taglineOffsetJob.join()

        // Hold the completed splash briefly so it doesn't flash away.
        delay(300)

        // Wait for session verification if it hasn't finished yet, then navigate.
        sessionDeferred.await()
        onEnd()
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
        // Radial glow halo behind the logo
        Box(
            modifier = Modifier
                .size(260.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Animated logo icon ─────────────────────────────────────────
            Image(
                painter = painterResource(Res.drawable.ai_book),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                        alpha = iconAlpha.value
                        rotationZ = iconRotation.value
                    }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Letter-by-letter app name ──────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                appName.forEachIndexed { i, letter ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.graphicsLayer {
                            translationY = letterOffsets[i].value
                            alpha = letterAlphas[i].value
                        }
                    ) {
                        // Soft glow layer (slightly scaled-up, translucent)
                        Text(
                            text = letter.toString(),
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary.copy(
                                alpha = glowAlpha * 0.7f
                            ),
                            letterSpacing = 2.sp,
                            modifier = Modifier.graphicsLayer {
                                scaleX = 1.12f
                                scaleY = 1.12f
                            }
                        )
                        // Crisp foreground letter
                        Text(
                            text = letter.toString(),
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Tagline ────────────────────────────────────────────────────
            Text(
                text = stringResource(Res.string.app_tagline),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp,
                modifier = Modifier.graphicsLayer {
                    translationY = taglineOffset.value
                    alpha = taglineAlpha.value
                }
            )
        }
    }
}
