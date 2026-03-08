package feature.study.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
internal data class ResponsiveSizes(
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
internal fun rememberResponsiveSizes(
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
