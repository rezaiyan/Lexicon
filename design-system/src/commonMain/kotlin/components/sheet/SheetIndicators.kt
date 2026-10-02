package components.sheet

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.step_of
import org.jetbrains.compose.resources.stringResource
import theme.Theme

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
