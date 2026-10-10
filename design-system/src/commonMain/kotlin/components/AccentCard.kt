package components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import theme.Theme

/**
 * Tappable surface card tinted by an [accent] colour — the shared shell for Study-tab
 * entry cards (practice modes, learning stages, tags).
 *
 * Enabled cards get a thin [accent] ring and low elevation; disabled ones are flat, dimmed
 * and not clickable. Built on [Surface] rather than `Card` so elevation follows [enabled]
 * after first composition (`Card` without `onClick` remembers its initial elevation).
 */
@Composable
fun AccentCard(
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Theme.shapes.large),
        color = MaterialTheme.colorScheme.surface,
        border = if (enabled) {
            BorderStroke(Theme.dimensions.borderWidth, accent.copy(alpha = Theme.opacity.dimming))
        } else {
            null
        },
        shadowElevation = if (enabled) Theme.elevation.low else Theme.elevation.none,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .alpha(if (enabled) 1f else Theme.opacity.hint)
                .padding(Theme.spacing.cardPadding),
        ) {
            content()
        }
    }
}

/** [icon] tinted [accent] inside a soft [accent] circle, as used at the start of an [AccentCard]. */
@Composable
fun AccentIconBadge(
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = Theme.dimensions.iconSizeHuge,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(accent.copy(alpha = Theme.opacity.focus), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(Theme.dimensions.iconSize),
        )
    }
}
