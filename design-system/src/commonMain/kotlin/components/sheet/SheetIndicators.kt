package components.sheet

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.step_of
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private val LevelBarWidth = 6.dp
private val LevelBarHeights = listOf(10.dp, 16.dp, 22.dp)

/** Three rising bars in a rounded tile; the first [filled] are lit. Pictures a proficiency level. */
@Composable
fun LevelBars(filled: Int, selected: Boolean, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val tile = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier = modifier
            .size(Theme.dimensions.touchTargetSmall + Theme.spacing.xxs)
            .clip(RoundedCornerShape(Theme.shapes.medium))
            .background(tile)
            .padding(bottom = Theme.spacing.sm - Theme.spacing.xxxs),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        LevelBarHeights.forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .width(LevelBarWidth)
                    .height(height)
                    .clip(CircleShape)
                    .background(if (index < filled) accent else MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

/** Radio indicator for cards that carry their own click handling (see [SelectableCard]). */
@Composable
fun RadioDot(selected: Boolean, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val ring by animateColorAsState(if (selected) accent else MaterialTheme.colorScheme.outline, label = "radio")
    Box(
        modifier = modifier
            .size(Theme.dimensions.iconSizeMedium + Theme.spacing.xxxs)
            .border(Theme.spacing.xxxs, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(Modifier.size(Theme.spacing.sm - Theme.spacing.xxxs).clip(CircleShape).background(accent))
        }
    }
}

/** Segmented progress for multi-step sheets plus its "Step X of Y" announcement. */
@Composable
fun StepProgressBar(current: Int, total: Int, modifier: Modifier = Modifier) {
    val label = stringResource(Res.string.step_of, current, total)
    Row(
        modifier = modifier.semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs + Theme.spacing.xxxs),
    ) {
        repeat(total) { index ->
            val color by animateColorAsState(
                if (index < current) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                label = "step_$index",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(Theme.dimensions.progressBarHeight)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/** Small rounded label, e.g. "Recommended" or "3 added". */
@Composable
fun SheetBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = contentColor,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .clip(RoundedCornerShape(Theme.shapes.pill))
            .background(containerColor)
            .padding(horizontal = Theme.spacing.xs, vertical = Theme.spacing.xxxs),
    )
}
