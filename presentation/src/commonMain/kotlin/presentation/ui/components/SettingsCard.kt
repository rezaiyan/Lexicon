package presentation.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import components.ListTile
import theme.Theme

/**
 * Settings-style card: leading icon, title/subtitle, optional trailing content.
 * Delegates to the design-system [ListTile].
 */
@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    iconTint: Color? = null,
    iconBackgroundColor: Color? = null,
    solidIconBackground: Boolean = false,
    subtitleColor: Color? = null,
    showTrailingArrow: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    ListTile(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        iconTint = iconTint,
        iconBackgroundColor = iconBackgroundColor,
        solidIconBackground = solidIconBackground,
        subtitleColor = subtitleColor,
        containerColor = Theme.colors.settingsCardBackground,
        trailingContent = trailingContent,
        showTrailingArrow = showTrailingArrow,
        trailingArrowIcon = Icons.Default.KeyboardArrowRight
    )
}
