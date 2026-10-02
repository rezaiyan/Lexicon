package presentation.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import components.GroupedRow

/**
 * Settings row: tinted leading icon, title/subtitle, optional trailing content.
 * Meant to sit inside a design-system [components.GroupedSection];
 * pass `showDivider = false` on the last row of a section.
 */
@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    iconTint: Color? = null,
    iconBackgroundColor: Color? = null,
    subtitleColor: Color? = null,
    showTrailingArrow: Boolean = true,
    showDivider: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    GroupedRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        icon = icon,
        iconColor = iconTint ?: iconBackgroundColor,
        subtitleColor = subtitleColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
        trailingContent = trailingContent,
        showChevron = showTrailingArrow,
        showDivider = showDivider,
    )
}
