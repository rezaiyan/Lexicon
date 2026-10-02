package feature.study.ui.review

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import domain.tts.model.TtsState
import domain.word.model.Word
import feature.study.model.ReviewType
import feature.study.ui.components.FlashCard
import theme.Theme

/**
 * Main review content area.
 *
 * Layout: top bar, then the card filling the free space, then the footer (Show answer ↔ rating, or
 * browse navigation) in the thumb zone. Cards slide horizontally between words (direction-aware);
 * a vertical swipe anywhere in the card slot flips the card.
 */
@Composable
fun ReviewContent(
    words: List<Word>,
    currentIndex: Int,
    isFlipped: Boolean,
    reviewType: ReviewType,
    onClose: () -> Unit,
    onFlip: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit,
    onReview: (Int) -> Unit,
    onEdit: (() -> Unit)? = null,
    ttsState: TtsState = TtsState.Idle,
    onSpeakClick: (text: String, langCode: String) -> Unit = { _, _ -> },
    isAutoPlayEnabled: Boolean = false,
    onAutoPlayToggle: (Boolean) -> Unit = {},
    speechRate: Float = 1.0f,
    onSpeechRateChanged: (Float) -> Unit = {},
) {
    // Swipe-up (front→back) or swipe-down (back→front) anywhere in the card slot
    // so one-handed users can trigger the flip from the bottom thumb zone.
    // rawDragY resets to 0 on release, spring-animating the card back to rest.
    var rawDragY by remember { mutableFloatStateOf(0f) }
    val dragFeedbackY by animateFloatAsState(
        targetValue = rawDragY.coerceIn(-28f, 28f),
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "dragFeedback"
    )
    val density = LocalDensity.current
    val flipThresholdPx = remember(density) { with(density) { 52.dp.toPx() } }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ReviewTopBar(
            currentIndex = currentIndex,
            totalCount = words.size,
            isAutoPlayEnabled = isAutoPlayEnabled,
            onAutoPlayToggle = onAutoPlayToggle,
            speechRate = speechRate,
            onSpeechRateChanged = onSpeechRateChanged,
            onClose = onClose,
        )

        // ── Card slot: fills all remaining vertical space ─────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.md)
                .pointerInput(isFlipped) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            val triggered = if (!isFlipped) rawDragY < -flipThresholdPx
                                           else rawDragY > flipThresholdPx
                            rawDragY = 0f
                            if (triggered) onFlip()
                        },
                        onDragCancel = { rawDragY = 0f },
                        onVerticalDrag = { _, delta -> rawDragY += delta }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    val goingForward = targetState > initialState
                    val enter = slideInHorizontally(
                        animationSpec = tween(380, easing = FastOutSlowInEasing),
                        initialOffsetX = { if (goingForward) it else -it }
                    ) + fadeIn(tween(280, delayMillis = 60))
                    val exit = slideOutHorizontally(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        targetOffsetX = { if (goingForward) -it else it }
                    ) + fadeOut(tween(200))
                    enter togetherWith exit
                },
                label = "cardSlide",
                modifier = Modifier.fillMaxSize()
            ) { index ->
                val word = words.getOrNull(index)
                if (word != null) {
                    FlashCard(
                        word = word,
                        isFlipped = isFlipped,
                        onFlip = onFlip,
                        dragFeedbackY = dragFeedbackY,
                        ttsState = ttsState,
                        onSpeakClick = onSpeakClick,
                        onEdit = onEdit,
                    )
                }
            }
        }

        // ── Action buttons ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Theme.spacing.md)
                .padding(bottom = Theme.spacing.md)
        ) {
            when (reviewType) {
                ReviewType.REVIEW -> ReviewRatingArea(isFlipped = isFlipped, onFlip = onFlip, onReview = onReview)
                ReviewType.BROWSE -> NavigationButtons(
                    currentIndex = currentIndex,
                    totalCount = words.size,
                    onNavigateBack = onNavigateBack,
                    onNavigateForward = onNavigateForward
                )
            }
        }
    }
}
