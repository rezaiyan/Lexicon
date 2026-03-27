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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import domain.tts.model.TtsState
import domain.word.model.Word
import feature.study.model.ReviewType
import feature.study.ui.components.FlashCard
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.edit
import lexicon.resources.generated.resources.tap_card_to_reveal
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/**
 * Main review content area.
 *
 * Animations:
 * - Cards slide in/out horizontally when navigating between words (direction-aware)
 * - A "tap to reveal" hint fades in/out in review mode before the card is flipped
 * - Rating buttons slide up after the card is flipped in review mode
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
    // Animate the "tap to reveal" hint alpha outside the nested Box lambda to
    // avoid Kotlin's implicit-receiver overload resolution picking ColumnScope.AnimatedVisibility.
    val hintAlpha by animateFloatAsState(
        targetValue = if (!isFlipped && reviewType == ReviewType.REVIEW) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "hintAlpha"
    )

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
                .padding(top = Theme.spacing.lg, bottom = Theme.spacing.xs)
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
                        onSpeakClick = onSpeakClick
                    )
                }
            }

            // "Tap to reveal" hint — pill chip
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Theme.spacing.sm)
                    .graphicsLayer { alpha = hintAlpha }
                    .clip(RoundedCornerShape(100.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = Theme.spacing.sm, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(Res.string.tap_card_to_reveal),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                )
            }

            // ── Edit word — overlaid icon at bottom-end of card slot ───────
            if (onEdit != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = Theme.spacing.sm, bottom = Theme.spacing.sm)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f))
                        .clickable(role = Role.Button, onClick = onEdit)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(Res.string.edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // ── Action buttons ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.lg)
                .padding(bottom = Theme.spacing.lg)
        ) {
            when (reviewType) {
                ReviewType.REVIEW -> ReviewRatingArea(isFlipped = isFlipped, onReview = onReview)
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
