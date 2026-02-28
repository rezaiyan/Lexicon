package presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.tts.model.TtsState
import domain.word.model.Word
import theme.Theme
import org.jetbrains.compose.resources.stringResource

import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.consolidating
import lexicon.resources.generated.resources.familiar
import lexicon.resources.generated.resources.learning
import lexicon.resources.generated.resources.mastered
import lexicon.resources.generated.resources.mature
import lexicon.resources.generated.resources.new
import lexicon.resources.generated.resources.unknown
import lexicon.resources.generated.resources.young

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
fun MasteryLevelBadge(level: Int, modifier: Modifier = Modifier) {
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

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(Theme.dimensions.hairlineThickness, masteryColor.copy(alpha = 0.3f), RoundedCornerShape(50))
            .background(masteryColor.copy(alpha = 0.12f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = masteryText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = masteryColor
        )
    }
}

// ── Speaker button ────────────────────────────────────────────────────────────

@Composable
private fun SpeakerButton(
    ttsState: TtsState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isActive =
        ttsState is TtsState.Speaking || ttsState is TtsState.Downloading || ttsState is TtsState.Loading

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (ttsState) {
            is TtsState.Downloading -> {
                CircularProgressIndicator(
                    progress = { ttsState.progress },
                    modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            is TtsState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            else -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Speak",
                    modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                    tint = if (ttsState is TtsState.Speaking) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Review action buttons ─────────────────────────────────────────────────────

@Composable
fun ReviewButton(
    text: String,
    subText: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val isVerySmall = maxWidth < 70.dp
        val isSmall = maxWidth < 90.dp

        androidx.compose.material3.Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isVerySmall) 60.dp else 72.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = color.copy(alpha = 0.2f),
                contentColor = color
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = if (isVerySmall) 2.dp else if (isSmall) 4.dp else 8.dp,
                vertical = 8.dp
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    style = when {
                        isVerySmall -> MaterialTheme.typography.labelSmall
                        isSmall -> MaterialTheme.typography.labelMedium
                        else -> MaterialTheme.typography.labelLarge
                    },
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subText,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.65f),
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Responsive sizing ─────────────────────────────────────────────────────────

@Immutable
private data class ResponsiveSizes(
    val titleSp: TextUnit,
    val titleMaxLines: Int,
    val descMaxLines: Int,
    val titleWidthFraction: Float,
    val descWidthFraction: Float,
    val contentPadding: Dp,
    val badgePadding: Dp,
    val afterTitleSpacing: Dp,
    val cardCornerRadius: Dp,
    val cardElevation: Dp,
    val cardHeight: Dp,
    val cameraDistancePx: Float
)

@Composable
private fun rememberResponsiveSizes(
    maxWidth: Dp,
    maxHeight: Dp,
    isLandscape: Boolean
): ResponsiveSizes {
    val density = LocalDensity.current
    val shortest = if (maxWidth < maxHeight) maxWidth else maxHeight

    val bucket = remember(shortest) {
        when {
            shortest < 360.dp -> SizeBucket.CompactXS
            shortest < 480.dp -> SizeBucket.CompactS
            shortest < 600.dp -> SizeBucket.Compact
            shortest < 840.dp -> SizeBucket.Medium
            else -> SizeBucket.Expanded
        }
    }

    val targetHeight = when {
        isLandscape -> maxHeight * 0.75f
        else -> maxHeight * 0.6f
    }.coerceIn(280.dp, 520.dp)

    val bucketValues = when (bucket) {
        SizeBucket.CompactXS -> SizeBucketValues(
            titleSp = 24.sp,
            titleLines = 1,
            descLines = 2,
            titleWidth = 0.96f,
            descWidth = 0.96f,
            contentPadding = 12.dp,
            badgePadding = 8.dp,
            afterTitleSpacing = 8.dp,
            cardCornerRadius = 14.dp,
            cardElevation = 4.dp
        )

        SizeBucket.CompactS -> SizeBucketValues(
            titleSp = 28.sp,
            titleLines = 1,
            descLines = 3,
            titleWidth = 0.94f,
            descWidth = 0.94f,
            contentPadding = 14.dp,
            badgePadding = 10.dp,
            afterTitleSpacing = 10.dp,
            cardCornerRadius = 16.dp,
            cardElevation = 6.dp
        )

        SizeBucket.Compact -> SizeBucketValues(
            titleSp = 32.sp,
            titleLines = 1,
            descLines = 3,
            titleWidth = 0.92f,
            descWidth = 0.92f,
            contentPadding = 16.dp,
            badgePadding = 12.dp,
            afterTitleSpacing = 12.dp,
            cardCornerRadius = 20.dp,
            cardElevation = 8.dp
        )

        SizeBucket.Medium -> SizeBucketValues(
            titleSp = 40.sp,
            titleLines = 1,
            descLines = 4,
            titleWidth = 0.9f,
            descWidth = 0.9f,
            contentPadding = 20.dp,
            badgePadding = 14.dp,
            afterTitleSpacing = 14.dp,
            cardCornerRadius = 24.dp,
            cardElevation = 10.dp
        )

        SizeBucket.Expanded -> SizeBucketValues(
            titleSp = 48.sp,
            titleLines = 1,
            descLines = 5,
            titleWidth = 0.85f,
            descWidth = 0.85f,
            contentPadding = 24.dp,
            badgePadding = 16.dp,
            afterTitleSpacing = 16.dp,
            cardCornerRadius = 28.dp,
            cardElevation = 12.dp
        )
    }

    val cameraDistancePx = with(density) { 12.dp.toPx() }

    return ResponsiveSizes(
        titleSp = bucketValues.titleSp,
        titleMaxLines = bucketValues.titleLines,
        descMaxLines = bucketValues.descLines,
        titleWidthFraction = bucketValues.titleWidth,
        descWidthFraction = bucketValues.descWidth,
        contentPadding = bucketValues.contentPadding,
        badgePadding = bucketValues.badgePadding,
        afterTitleSpacing = bucketValues.afterTitleSpacing,
        cardCornerRadius = bucketValues.cardCornerRadius,
        cardElevation = bucketValues.cardElevation,
        cardHeight = targetHeight,
        cameraDistancePx = cameraDistancePx
    )
}

private enum class SizeBucket {
    CompactXS, CompactS, Compact, Medium, Expanded
}

@Immutable
private data class SizeBucketValues(
    val titleSp: TextUnit,
    val titleLines: Int,
    val descLines: Int,
    val titleWidth: Float,
    val descWidth: Float,
    val contentPadding: Dp,
    val badgePadding: Dp,
    val afterTitleSpacing: Dp,
    val cardCornerRadius: Dp,
    val cardElevation: Dp
)
