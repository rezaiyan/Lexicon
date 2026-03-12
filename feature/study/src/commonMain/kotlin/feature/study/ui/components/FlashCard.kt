package feature.study.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.tts.model.TtsState
import domain.word.model.Word
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.consolidating
import lexicon.resources.generated.resources.familiar
import lexicon.resources.generated.resources.learning
import lexicon.resources.generated.resources.mastered
import lexicon.resources.generated.resources.mature
import lexicon.resources.generated.resources.new
import lexicon.resources.generated.resources.repeat_pronunciation
import lexicon.resources.generated.resources.unknown
import lexicon.resources.generated.resources.young
import org.jetbrains.compose.resources.stringResource

@Composable
fun FlashCard(
    word: Word,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
    ttsState: TtsState = TtsState.Idle,
    onSpeakClick: (text: String, langCode: String) -> Unit = { _, _ -> }
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "cardFlip"
    )

    // Front face = slightly elevated surface so it stands out from the background.
    // Back face = primaryContainer to signal "answer revealed".
    val cardColor by animateColorAsState(
        targetValue = if (rotation > 90f) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "cardColor"
    )
    val mainTextColor by animateColorAsState(
        targetValue = if (rotation > 90f) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "mainTextColor"
    )

    // Press-to-scale for tactile flip feedback.
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "cardScale"
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isLandscape = maxWidth > maxHeight
        val sizes = rememberResponsiveSizes(
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            isLandscape = isLandscape
        )

        // Alpha for back-face elements: fades in as the card reveals its back.
        val backFaceAlpha = ((rotation - 90f) / 90f).coerceIn(0f, 1f)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(sizes.cardHeight)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = sizes.cameraDistancePx
                    scaleX = cardScale
                    scaleY = cardScale
                },
            shape = RoundedCornerShape(sizes.cardCornerRadius),
            elevation = CardDefaults.cardElevation(sizes.cardElevation),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            // clickable is placed here — inside the Card — so the ripple is clipped
            // to the card's rounded shape by Card's own shape clip, not the outer rect.
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current
                    ) { onFlip() }
            ) {
                // Level badge — top-end: always-visible context, never competes with the word
                MasteryLevelBadge(
                    level = word.level,
                    isBackFace = rotation > 90f,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(sizes.badgePadding)
                        .graphicsLayer { rotationY = if (rotation > 90f) 180f else 0f }
                )

                // Speaker button — bottom-start
                if (!isFlipped) {
                    SpeakerButton(
                        ttsState = ttsState,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(sizes.badgePadding),
                        onClick = {
                            onSpeakClick(word.originalWord, word.targetLanguage.code)
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(sizes.contentPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        val textToDisplay =
                            if (rotation <= 90f) word.originalWord else word.translation

                        val dynamicMaxSize = sizes.titleSp * 1.5f
                        val minFontSizeForTwoLines = 11.sp

                        var allowTwoLines by remember { mutableStateOf(false) }

                        Text(
                            text = textToDisplay,
                            fontSize = dynamicMaxSize,
                            fontWeight = FontWeight.Bold,
                            color = mainTextColor,
                            textAlign = TextAlign.Center,
                            maxLines = if (allowTwoLines) 2 else 1,
                            overflow = TextOverflow.Visible,
                            style = MaterialTheme.typography.displayMedium,
                            autoSize = if (!allowTwoLines) {
                                TextAutoSize.StepBased(
                                    minFontSize = minFontSizeForTwoLines,
                                    maxFontSize = dynamicMaxSize,
                                    stepSize = 1.sp
                                )
                            } else {
                                TextAutoSize.StepBased(
                                    minFontSize = 8.sp,
                                    maxFontSize = minFontSizeForTwoLines,
                                    stepSize = 1.sp
                                )
                            },
                            onTextLayout = { layoutResult ->
                                if (!allowTwoLines && layoutResult.didOverflowWidth) {
                                    allowTwoLines = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth(sizes.titleWidthFraction)
                                .graphicsLayer {
                                    rotationY = if (rotation > 90f) 180f else 0f
                                }
                        )
                    }

                    // Description fades in as the back face finishes revealing.
                    if (word.description.isNotBlank() && rotation > 90f) {
                        Spacer(Modifier.height(sizes.afterTitleSpacing))
                        Text(
                            text = word.description,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = mainTextColor.copy(alpha = 0.75f),
                            maxLines = sizes.descMaxLines,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth(sizes.descWidthFraction)
                                .graphicsLayer {
                                    rotationY = 180f
                                    alpha = backFaceAlpha
                                }
                        )
                    }
                }
            }
        }
    }
}

// ── Mastery level badge — lightweight pill chip ───────────────────────────────

@Composable
fun MasteryLevelBadge(level: Int, isBackFace: Boolean = false, modifier: Modifier = Modifier) {
    val (masteryText, masteryColor) = when (level) {
        0 -> Pair(stringResource(Res.string.new), MaterialTheme.colorScheme.secondary)
        1 -> Pair(stringResource(Res.string.learning), MaterialTheme.colorScheme.tertiary)
        2 -> Pair(stringResource(Res.string.familiar), MaterialTheme.colorScheme.primary)
        3 -> Pair(stringResource(Res.string.consolidating), MaterialTheme.colorScheme.primary)
        4 -> Pair(stringResource(Res.string.young), MaterialTheme.colorScheme.primary)
        5 -> Pair(stringResource(Res.string.mature), MaterialTheme.colorScheme.tertiary)
        6 -> Pair(stringResource(Res.string.mastered), MaterialTheme.colorScheme.secondary)
        else -> Pair(stringResource(Res.string.unknown), MaterialTheme.colorScheme.surfaceVariant)
    }

    // On the back face (primaryContainer), use onPrimaryContainer so the label
    // always contrasts with the card background instead of blending with the word text.
    val displayColor = if (isBackFace) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.45f)
    } else {
        masteryColor.copy(alpha = 0.7f)
    }

    Text(
        text = masteryText,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = displayColor,
        modifier = modifier
    )
}

// ── Speaker button with pulse & loading overlay ──────────────────────────────

@Composable
private fun SpeakerButton(
    ttsState: TtsState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isSpeaking = ttsState is TtsState.Speaking
    val isLoading = ttsState is TtsState.Loading || ttsState is TtsState.Downloading

    // Press-to-scale feedback
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "speakerPress"
    )

    // Breathing pulse while speaking — always cycling, gated by pulseGate
    val infiniteTransition = rememberInfiniteTransition(label = "speakerPulse")
    val pulseRaw by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRaw"
    )
    val pulseGate by animateFloatAsState(
        targetValue = if (isSpeaking) 1f else 0f,
        animationSpec = tween(300),
        label = "pulseGate"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            isSpeaking -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            isLoading -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        },
        animationSpec = tween(300),
        label = "speakerBg"
    )
    val iconTint by animateColorAsState(
        targetValue = when {
            isSpeaking -> MaterialTheme.colorScheme.primary
            isLoading -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        },
        animationSpec = tween(300),
        label = "speakerTint"
    )

    val scale = pressScale * (1f + pulseRaw * 0.10f * pulseGate)

    Box(
        modifier = modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Icon — always visible, dimmed when a loading spinner is overlaid
        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = stringResource(Res.string.repeat_pronunciation),
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    val s = if (isLoading) 0.75f else 1f
                    scaleX = s
                    scaleY = s
                    alpha = if (isLoading) 0.4f else 1f
                },
            tint = iconTint
        )

        // Loading ring overlaid on the icon
        when (ttsState) {
            is TtsState.Downloading -> {
                CircularProgressIndicator(
                    progress = { ttsState.progress },
                    modifier = Modifier.size(34.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            is TtsState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(34.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            else -> {}
        }
    }
}
