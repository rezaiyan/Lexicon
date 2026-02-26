package presentation.ui.screens.review

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_PEEK_CARDS = 3
private const val STAGGER_DELAY_MS = 130L
private const val CARD_ENTRY_DURATION_MS = 520
private const val SETTLE_PAUSE_MS = 250L

/** Session-scoped flag — the deck intro plays at most once per app launch. */
internal var deckAnimationPlayed: Boolean = false
    private set

internal fun markDeckAnimationPlayed() {
    deckAnimationPlayed = true
}

/**
 * Background deck cards rendered behind the active FlashCard.
 *
 * When [animate] is true, cards slide in from below-right with stagger,
 * settle into a fanned stack, and remain visible. When false, cards are
 * rendered immediately in their settled positions.
 *
 * Place this composable **before** the FlashCard inside the same Box so
 * the deck peeks out behind the active card.
 */
@Composable
internal fun DeckPeekCards(
    cardCount: Int,
    animate: Boolean,
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    val peekCards = (cardCount - 1).coerceIn(0, MAX_PEEK_CARDS)
    if (peekCards == 0) return

    val density = LocalDensity.current
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val accentTint = MaterialTheme.colorScheme.tertiaryContainer
    val borderBase = MaterialTheme.colorScheme.outlineVariant

    val cardProgress = remember {
        List(peekCards) { Animatable(if (animate) 0f else 1f) }
    }

    if (animate) {
        LaunchedEffect(Unit) {
            cardProgress.forEachIndexed { index, anim ->
                launch {
                    delay(index * STAGGER_DELAY_MS)
                    anim.animateTo(
                        1f,
                        tween(CARD_ENTRY_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                }
            }

            delay(
                (peekCards - 1) * STAGGER_DELAY_MS +
                        CARD_ENTRY_DURATION_MS + SETTLE_PAUSE_MS
            )

            markDeckAnimationPlayed()
            onAnimationComplete()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val cardHeight = (if (isLandscape) maxHeight * 0.75f else maxHeight * 0.6f)
            .coerceIn(280.dp, 520.dp)

        for (cardIndex in 0 until peekCards) {
            val depth = peekCards - cardIndex // deepest first (3, 2, 1)
            val depthFraction = depth.toFloat() / peekCards

            // Settled offsets — each card peeks out clearly
            val settledY = depth * 14f
            val settledX = depth * 4f
            val scaleReduction = depth * 0.032f
            val baseAlpha = 1f - depth * 0.12f
            val settledRotation = depth * 1.0f

            // Deeper cards tint toward tertiary accent
            val containerColor = lerp(surfaceHigh, accentTint, depthFraction * 0.4f)
            val borderColor = borderBase.copy(alpha = 0.2f + depthFraction * 0.15f)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        val progress = cardProgress[cardIndex].value

                        // Entry: sweep from bottom-right (deeper cards travel further)
                        val entryY = (1f - progress) * (160f + depth * 30f)
                        val entryX = (1f - progress) * (30f + depth * 20f)
                        val entryRotation = (1f - progress) * depth * 4f

                        translationY = with(density) { (settledY + entryY).dp.toPx() }
                        translationX = with(density) { (settledX + entryX).dp.toPx() }
                        scaleX = 1f - scaleReduction
                        scaleY = 1f - scaleReduction
                        alpha = baseAlpha * progress
                        rotationZ = (settledRotation + entryRotation) * progress
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = (8 - depth * 2).coerceAtLeast(2).dp
                ),
                border = BorderStroke(0.5.dp, borderColor)
            ) { /* empty shell */ }
        }
    }
}
