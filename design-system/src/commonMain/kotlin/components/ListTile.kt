package components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import theme.Theme

/**
 * A clickable card row with a leading icon, title/subtitle text, and optional trailing content.
 *
 * This is the standard list-item pattern used for settings rows, navigation items,
 * and any card that follows the icon → text → action layout.
 *
 * @param icon Leading icon vector.
 * @param title Primary text.
 * @param onClick Card click handler.
 * @param modifier Optional modifier.
 * @param subtitle Optional secondary text below the title.
 * @param iconTint Tint for the leading icon. Defaults to current content color.
 * @param iconBackgroundColor When provided, the icon is placed inside a circular background
 *        filled with this color at 12% opacity.
 * @param subtitleColor Color for the subtitle text. Defaults to onSurfaceVariant.
 * @param trailingContent Optional composable slot on the right side. When null and
 *        [showTrailingArrow] is true, a default forward arrow icon is shown.
 * @param showTrailingArrow Whether to show a default trailing arrow when [trailingContent] is null.
 */
@Composable
fun ListTile(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: Color? = null,
    iconBackgroundColor: Color? = null,
    solidIconBackground: Boolean = false,
    subtitleColor: Color? = null,
    containerColor: Color? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    showTrailingArrow: Boolean = true,
    trailingArrowIcon: ImageVector? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = if (containerColor != null) {
            CardDefaults.cardColors(containerColor = containerColor)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (iconBackgroundColor != null) {
                    val bgColor = if (solidIconBackground) {
                        iconBackgroundColor
                    } else {
                        iconBackgroundColor.copy(alpha = 0.12f)
                    }
                    val fgColor = if (solidIconBackground) {
                        iconTint ?: Color.White
                    } else {
                        iconTint ?: iconBackgroundColor
                    }
                    Box(
                        modifier = Modifier
                            .size(Theme.dimensions.touchTargetSmall)
                            .clip(CircleShape)
                            .background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(Theme.dimensions.iconSize),
                            tint = fgColor
                        )
                    }
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(Theme.dimensions.iconSize),
                        tint = iconTint ?: LocalContentColor.current
                    )
                }

                Spacer(modifier = Modifier.width(Theme.spacing.sm))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (subtitle != null) {
                        Text(
                            modifier = Modifier.padding(end = Theme.spacing.xs),
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = subtitleColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (trailingContent != null) {
                trailingContent()
            } else if (showTrailingArrow && trailingArrowIcon != null) {
                Icon(
                    imageVector = trailingArrowIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
