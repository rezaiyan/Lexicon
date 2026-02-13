package presentation.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import org.kodein.emoji.compose.m3.TextWithNotoImageEmoji
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.word.model.Word
import org.jetbrains.compose.resources.stringResource
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.consolidating
import vokab.resources.generated.resources.edit
import vokab.resources.generated.resources.familiar
import vokab.resources.generated.resources.learning
import vokab.resources.generated.resources.level_format
import vokab.resources.generated.resources.mastered
import vokab.resources.generated.resources.mature
import vokab.resources.generated.resources.new
import vokab.resources.generated.resources.unknown
import vokab.resources.generated.resources.young

@Composable
fun FlashCard(
    word: Word,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        )
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isLandscape = maxWidth > maxHeight
        val sizes = rememberResponsiveSizes(
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            isLandscape = isLandscape
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(sizes.cardHeight)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = sizes.cameraDistancePx
                }
                .clickable { onFlip() },
            shape = RoundedCornerShape(sizes.cardCornerRadius),
            elevation = CardDefaults.cardElevation(sizes.cardElevation),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .padding(sizes.badgePadding)
                                .graphicsLayer { rotationY = if (rotation > 90f) 180f else 0f }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(Res.string.edit),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    } else {
                        Spacer(Modifier.width(sizes.badgePadding))
                    }

                    MasteryLevelBadge(
                        level = word.level,
                        modifier = Modifier
                            .padding(sizes.badgePadding)
                            .graphicsLayer { rotationY = if (rotation > 90f) 180f else 0f }
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
                        val textToDisplay = if (rotation <= 90f) {
                            word.originalWord
                        } else {
                            word.translation
                        }

                        val dynamicMaxSize = sizes.titleSp * 1.5f
                        val minFontSizeForTwoLines = 11.sp

                        var allowTwoLines by remember { mutableStateOf(false) }

                        Text(
                            text = textToDisplay,
                            fontSize = dynamicMaxSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                            modifier = Modifier.fillMaxWidth(sizes.titleWidthFraction)
                                .graphicsLayer {
                                    rotationY = if (rotation > 90f) 180f else 0f
                                }
                        )
                    }

                    if (word.description.isNotBlank() && rotation > 90f) {
                        Spacer(Modifier.height(sizes.afterTitleSpacing))
                        Text(
                            text = word.description,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            maxLines = sizes.descMaxLines,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth(sizes.descWidthFraction)
                                .graphicsLayer { rotationY = 180f }
                        )
                    }
                }
            }
        }
    }

}

@Composable
fun MasteryLevelBadge(level: Int, modifier: Modifier = Modifier) {
    // 7-Bucket System (Levels 0-6)
    val (masteryText, masteryColor, masteryIcon) = when (level) {
        0 -> Triple(stringResource(Res.string.new), MaterialTheme.colorScheme.secondary, "📝")
        1 -> Triple(
            stringResource(Res.string.learning),
            MaterialTheme.colorScheme.tertiary,
            "📚"
        )

        2 -> Triple(stringResource(Res.string.familiar), MaterialTheme.colorScheme.primary, "💡")
        3 -> Triple(
            stringResource(Res.string.consolidating),
            MaterialTheme.colorScheme.primary,
            "✨"
        )

        4 -> Triple(stringResource(Res.string.young), MaterialTheme.colorScheme.primary, "🌱")
        5 -> Triple(stringResource(Res.string.mature), MaterialTheme.colorScheme.tertiary, "🌟")
        6 -> Triple(
            stringResource(Res.string.mastered),
            MaterialTheme.colorScheme.primaryContainer,
            "👑"
        )

        else -> Triple(
            stringResource(Res.string.unknown),
            MaterialTheme.colorScheme.surfaceVariant,
            "❓"
        )
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = masteryColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 6.dp
            ), // Compact tap hint padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextWithNotoImageEmoji(
                    text = masteryIcon,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = masteryText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(
                text = stringResource(Res.string.level_format, level + 1),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ReviewButton(
    text: String,
    subText: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val isVerySmall = maxWidth < 70.dp
        val isSmall = maxWidth < 90.dp

        androidx.compose.material3.Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isVerySmall) 60.dp else 72.dp), // Dynamic height based on content
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = color.copy(alpha = 0.15f),
                contentColor = color
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
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
                    color = color.copy(alpha = 0.7f),
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

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
    CompactXS,
    CompactS,
    Compact,
    Medium,
    Expanded
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
