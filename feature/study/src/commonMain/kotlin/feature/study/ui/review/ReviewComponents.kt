package feature.study.ui.review

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import components.CounterPill
import components.GradientProgressBar
import domain.tts.model.TtsState
import domain.word.model.Word
import feature.study.model.ReviewType
import feature.study.ui.components.FlashCard
import feature.study.ui.components.ReviewButton
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.advance
import lexicon.resources.generated.resources.auto_play
import lexicon.resources.generated.resources.close
import lexicon.resources.generated.resources.did_you_remember
import lexicon.resources.generated.resources.edit
import lexicon.resources.generated.resources.forgot
import lexicon.resources.generated.resources.remembered
import lexicon.resources.generated.resources.restart
import lexicon.resources.generated.resources.tap_card_to_reveal
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/**
 * Compact top bar: close button (left), session title (center), card counter chip (right).
 * A full-bleed gradient progress strip runs along the bottom edge — no horizontal padding
 * so it spans edge-to-edge and feels like a native reading indicator.
 */
@Composable
private fun ReviewTopBar(
    currentIndex: Int,
    totalCount: Int,
    isAutoPlayEnabled: Boolean,
    onAutoPlayToggle: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val progress = (currentIndex + 1).toFloat() / totalCount.toFloat()

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Theme.spacing.extraSmall2,
                    end = Theme.spacing.medium,
                    top = Theme.spacing.extraSmall3,
                    bottom = Theme.spacing.extraSmall3
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(Res.string.close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.weight(1F))

            AutoPlayToggle(
                enabled = isAutoPlayEnabled,
                onToggle = onAutoPlayToggle
            )
            Spacer(Modifier.size(Theme.spacing.md))

            CounterPill(text = "${currentIndex + 1} / $totalCount")
        }

        GradientProgressBar(
            progress = progress,
            gradientColors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary
            ),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            animationDurationMs = 350
        )
    }
}

/**
 * Auto-play toggle with morphing icon animation.
 *
 * - Icon morphs between VolumeOff <-> VolumeUp with a spring-bounced crossfade
 * - Background transitions to primaryContainer when active
 * - Subtle breathing scale pulse indicates continuous playback
 */
@Composable
private fun AutoPlayToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.primaryContainer
        else Color.Transparent,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "autoPlayBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "autoPlayContent"
    )

    // Breathing pulse cycle — always running, gated by breathAmount
    val infiniteTransition = rememberInfiniteTransition(label = "autoPlayBreath")
    val breathCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathCycle"
    )
    val breathAmount by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = tween(300),
        label = "breathAmount"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                val pulse = 1f + breathCycle * 0.08f * breathAmount
                scaleX = pulse
                scaleY = pulse
            }
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onToggle(!enabled) }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = enabled,
            transitionSpec = {
                (scaleIn(
                    initialScale = 0.6f,
                    animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f)
                ) + fadeIn(tween(150)))
                    .togetherWith(
                        scaleOut(
                            targetScale = 0.6f,
                            animationSpec = tween(150)
                        ) + fadeOut(tween(100))
                    )
                    .using(SizeTransform(clip = false))
            },
            label = "autoPlayIcon"
        ) { isEnabled ->
            Icon(
                imageVector = if (isEnabled) Icons.AutoMirrored.Filled.VolumeUp
                else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = stringResource(Res.string.auto_play),
                modifier = Modifier.size(22.dp),
                tint = contentColor
            )
        }
    }
}

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

/**
 * Review-mode rating area.
 *
 * The button row always occupies 68 dp so the layout never shifts.
 * When the card is not yet flipped, the buttons fade + slide down out of view
 * using [graphicsLayer] (which doesn't affect measured size).
 * Once flipped, they ease back to their natural position — nudging the user
 * to rate only after seeing the answer.
 */
@Composable
private fun ReviewRatingArea(
    isFlipped: Boolean,
    onReview: (Int) -> Unit
) {
    val buttonAlpha by animateFloatAsState(
        targetValue = if (isFlipped) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "buttonAlpha"
    )
    val buttonTranslationY by animateFloatAsState(
        targetValue = if (isFlipped) 0f else 32f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "buttonSlide"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall3)
    ) {
        Text(
            text = stringResource(Res.string.did_you_remember),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Theme.spacing.extraSmall3)
        )

        // Row always has its full 68 dp height; graphicsLayer moves/fades it
        // without affecting sibling layout.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .graphicsLayer {
                    alpha = buttonAlpha
                    translationY = buttonTranslationY
                },
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing)
        ) {
            RatingButtonData.values.forEach { data ->
                ReviewButton(
                    text = stringResource(data.textRes),
                    subText = stringResource(data.subTextRes),
                    color = data.color(),
                    modifier = Modifier.weight(1f),
                    enabled = isFlipped,
                    onClick = { onReview(data.rating) }
                )
            }
        }
    }
}

internal data class RatingButtonData(
    val rating: Int,
    val textRes: org.jetbrains.compose.resources.StringResource,
    val subTextRes: org.jetbrains.compose.resources.StringResource,
    val color: @Composable () -> Color
) {
    companion object {
        val values = listOf(
            RatingButtonData(
                rating = 0,
                textRes = Res.string.forgot,
                subTextRes = Res.string.restart,
                color = { MaterialTheme.colorScheme.error }
            ),
            RatingButtonData(
                rating = 1,
                textRes = Res.string.remembered,
                subTextRes = Res.string.advance,
                color = { MaterialTheme.colorScheme.primary }
            )
        )
    }
}
