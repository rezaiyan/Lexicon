package components.sheet

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import theme.Theme

private val TileSize = 40.dp
private val SelectedBorder = 2.dp

/** Bordered container for a vertical list of rows ([SheetOptionRow], [SheetRadioRow], [SheetSwitchRow]). */
@Composable
fun SheetGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.large))
            .border(
                Theme.dimensions.borderWidth,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay),
                RoundedCornerShape(Theme.shapes.large),
            ),
        content = content,
    )
}

/** Navigational row: icon tile, title + optional subtitle, optional [trailingContent] (e.g. a badge), chevron. */
@Composable
fun SheetOptionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    showChevron: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    SheetRowLayout(
        title = title,
        subtitle = subtitle,
        showDivider = showDivider,
        titleColor = titleColor,
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        leading = { IconTile(icon = icon) },
        accessory = trailingContent,
        trailing = {
            if (showChevron) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                )
            }
        },
    )
}

/** Single-choice row; the whole row is the touch target. */
@Composable
fun SheetRadioRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
) {
    SheetRowLayout(
        title = title,
        subtitle = subtitle,
        showDivider = showDivider,
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        leading = icon?.let { { IconTile(icon = it, tinted = selected) } },
        trailing = { RadioButton(selected = selected, onClick = null) },
    )
}

/** Multi-choice row; the whole row toggles the checkbox. */
@Composable
fun SheetCheckboxRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
) {
    SheetRowLayout(
        title = title,
        subtitle = subtitle,
        showDivider = showDivider,
        modifier = modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        leading = icon?.let { { IconTile(icon = it, tinted = checked) } },
        trailing = { Checkbox(checked = checked, onCheckedChange = null) },
    )
}

/** On/off row; the whole row toggles. */
@Composable
fun SheetSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    SheetRowLayout(
        title = title,
        subtitle = subtitle,
        showDivider = showDivider,
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        leading = icon?.let { { IconTile(icon = it, tinted = checked && enabled) } },
        trailing = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
    )
}

/**
 * Read-only label / value row for detail sheets. The label is capped at 45% of the row so a long value
 * can't squeeze it; the value fills the rest, right-aligned, wrapping to three lines.
 */
@Composable
fun SheetInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val labelMaxWidth = maxWidth * InfoLabelMaxFraction
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Theme.dimensions.touchTarget + Theme.spacing.md)
                    .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
            ) {
                IconTile(icon)
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(max = labelMaxWidth),
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay),
                modifier = Modifier.padding(start = Theme.spacing.md),
            )
        }
    }
}

private const val InfoLabelMaxFraction = 0.45f
private const val AccessoryMaxFraction = 0.35f
private val CompactRowWidth = 300.dp

@Composable
private fun SheetRowLayout(
    title: String,
    subtitle: String?,
    showDivider: Boolean,
    modifier: Modifier,
    leading: (@Composable () -> Unit)?,
    trailing: @Composable RowScope.() -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    accessory: (@Composable () -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // On narrow rows / large font scales the accessory (badges, buttons) moves under the title
            // instead of squeezing it; otherwise it is capped so a wide accessory can't do the same.
            val stackAccessory = maxWidth / LocalDensity.current.fontScale < CompactRowWidth
            val accessoryMaxWidth = maxWidth * AccessoryMaxFraction
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = Theme.dimensions.touchTarget + Theme.spacing.md)
                    .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
            ) {
                leading?.invoke()
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs),
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                    )
                    subtitle?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (stackAccessory) accessory?.invoke()
                }
                if (!stackAccessory) {
                    accessory?.let {
                        Box(modifier = Modifier.widthIn(max = accessoryMaxWidth)) { it() }
                    }
                }
                trailing()
            }
        }
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay),
                modifier = Modifier.padding(start = Theme.spacing.md),
            )
        }
    }
}

/**
 * 40dp rounded square holding an icon.
 * [selected] fills it with the accent; [tinted] uses a soft accent wash (selected radio / switch rows).
 */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    tinted: Boolean = false,
) {
    val accent = MaterialTheme.colorScheme.primary
    val (container, content) = when {
        selected -> accent to MaterialTheme.colorScheme.onPrimary
        tinted -> accent.copy(alpha = Theme.opacity.focus) to accent
        else -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .size(TileSize)
            .clip(RoundedCornerShape(Theme.shapes.medium))
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(Theme.dimensions.iconSizeMedium))
    }
}

/** Card with a 2dp accent border and tint when selected; 1dp hairline otherwise. */
@Composable
fun SelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.Button,
    content: @Composable RowScope.() -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val container by animateColorAsState(
        if (selected) accent.copy(alpha = Theme.opacity.focus) else MaterialTheme.colorScheme.surface,
        label = "selectable_bg",
    )
    val borderColor by animateColorAsState(
        if (selected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay),
        label = "selectable_border",
    )
    Surface(
        onClick = onClick,
        modifier = modifier.semantics {
            this.selected = selected
            this.role = role
        },
        shape = RoundedCornerShape(Theme.shapes.large),
        color = container,
        border = BorderStroke(if (selected) SelectedBorder else Theme.dimensions.borderWidth, borderColor),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = Theme.dimensions.touchTarget + Theme.spacing.sm)
                .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xs + Theme.spacing.xxxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs + Theme.spacing.xxxs),
            content = content,
        )
    }
}
