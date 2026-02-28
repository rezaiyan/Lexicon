package components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import theme.Theme

/**
 * A small colored badge/pill for displaying short labels like language codes,
 * level names, counters, or status indicators.
 *
 * @param text The label text to display.
 * @param color The foreground text color. Background defaults to [color] at [backgroundAlpha].
 * @param modifier Optional modifier.
 * @param backgroundColor Explicit background color. When null, [color].copy(alpha = [backgroundAlpha]) is used.
 * @param backgroundAlpha Alpha applied to [color] for the default background. Ignored when [backgroundColor] is set.
 * @param fontWeight Text weight. Defaults to [FontWeight.Bold].
 * @param height Pill height. Defaults to 20.dp.
 * @param cornerRadius Corner radius. Defaults to pill shape (50%).
 */
@Composable
fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    backgroundAlpha: Float = 0.12f,
    fontWeight: FontWeight = FontWeight.Bold,
    height: Dp = 20.dp,
    cornerRadius: Dp = 10.dp
) {
    Surface(
        modifier = modifier.height(height),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor ?: color.copy(alpha = backgroundAlpha)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = Theme.spacing.xs),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = fontWeight,
                color = color
            )
        }
    }
}

/**
 * Larger pill variant for counter badges (e.g. "3 / 12", "24").
 * Uses surfaceVariant background by default.
 */
@Composable
fun CounterPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    fontWeight: FontWeight = FontWeight.SemiBold
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Theme.shapes.pill),
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier.padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xxs),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = fontWeight,
                color = color
            )
        }
    }
}
