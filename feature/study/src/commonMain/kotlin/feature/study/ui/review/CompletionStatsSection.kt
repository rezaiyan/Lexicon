package feature.study.ui.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import components.animation.rememberAnimatedCounter
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.completion_forgot
import lexicon.resources.generated.resources.completion_remembered
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/** Remembered / forgot split bar above two stat tiles. */
@Composable
internal fun StatsSection(
    knownCount: Int,
    unknownCount: Int,
    modifier: Modifier = Modifier,
) {
    val animatedKnown = rememberAnimatedCounter(knownCount)
    val animatedUnknown = rememberAnimatedCounter(unknownCount)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        SplitBar(knownCount = knownCount, unknownCount = unknownCount)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            StatTile(
                count = animatedKnown,
                label = stringResource(Res.string.completion_remembered),
                icon = Icons.Default.Check,
                accentColor = Theme.colors.success,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                count = animatedUnknown,
                label = stringResource(Res.string.completion_forgot),
                icon = Icons.Default.Close,
                accentColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SplitBar(knownCount: Int, unknownCount: Int) {
    val total = knownCount + unknownCount
    if (total == 0) return
    val knownFraction = knownCount.toFloat() / total
    val knownPercent = (knownFraction * 100).toInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Theme.spacing.xs)
            .semantics {
                contentDescription = "Results: $knownCount remembered ($knownPercent%), $unknownCount forgot"
            },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (knownCount > 0) BarSegment(weight = knownFraction, color = Theme.colors.success)
        if (unknownCount > 0) BarSegment(weight = 1f - knownFraction, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BarSegment(weight: Float, color: Color) {
    Box(
        modifier = Modifier
            .weight(weight)
            .height(Theme.spacing.xs)
            .clip(RoundedCornerShape(Theme.shapes.pill))
            .background(color),
    )
}

@Composable
private fun StatTile(
    count: Int,
    label: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Theme.shapes.large),
        color = with(MaterialTheme.colorScheme) {
            if (surface.luminance() < 0.5f) surfaceContainerHigh else surfaceContainerLowest
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(Theme.shapes.medium))
                    .background(accentColor.copy(alpha = Theme.opacity.focus)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
