package feature.study.ui.review

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
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
) {
    // Animate the "tap to reveal" hint alpha outside the nested Box lambda to
    // avoid Kotlin's implicit-receiver overload resolution picking ColumnScope.AnimatedVisibility.
    val hintAlpha by animateFloatAsState(
        targetValue = if (!isFlipped && reviewType == ReviewType.REVIEW) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "hintAlpha"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ReviewTopBar(
            currentIndex = currentIndex,
            totalCount = words.size,
            isAutoPlayEnabled = isAutoPlayEnabled,
            onAutoPlayToggle = onAutoPlayToggle,
            onClose = onClose
        )

        // ── Card slot: fills all remaining vertical space ─────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.medium)
                .padding(top = Theme.spacing.medium, bottom = Theme.spacing.extraSmall2),
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
                        ttsState = ttsState,
                        onSpeakClick = onSpeakClick
                    )
                }
            }

            // "Tap to reveal" hint
            Text(
                text = stringResource(Res.string.tap_card_to_reveal),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Theme.spacing.extraSmall2)
                    .graphicsLayer { alpha = hintAlpha }
            )
        }

        // ── Edit word ─────────────────────────────────────────────────────
        if (onEdit != null) {
            Row(
                modifier = Modifier
                    .padding(vertical = Theme.spacing.extraSmall3)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onEdit)
                    .padding(horizontal = Theme.spacing.sm, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(Res.string.edit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = stringResource(Res.string.edit),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            }
        }

        // ── Action buttons ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.medium)
                .padding(bottom = Theme.spacing.medium)
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
