package components.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import theme.Theme

/** Filled pill (buttonHeight min). Pass error colors for destructive actions. */
@Composable
fun SheetPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.fillMaxWidth().heightIn(min = Theme.dimensions.buttonHeight),
        shape = RoundedCornerShape(Theme.shapes.pill),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                color = contentColor,
                strokeWidth = Theme.spacing.xxxs,
            )
        } else {
            ButtonLabel(text, icon)
        }
    }
}

@Composable
fun SheetTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = Theme.dimensions.buttonHeight),
        shape = RoundedCornerShape(Theme.shapes.pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        ButtonLabel(text, icon)
    }
}

@Composable
fun SheetTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = Theme.dimensions.touchTarget),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

/**
 * Footer row: [secondary] keeps its intrinsic width, [primary] takes the rest.
 * Fixed 1:2 weights squeeze short labels ("Done") onto two lines at large font scales.
 */
@Composable
fun SheetFooterRow(
    secondary: @Composable (Modifier) -> Unit,
    primary: @Composable RowScope.(Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        secondary(Modifier.width(IntrinsicSize.Max))
        primary(Modifier.weight(1f))
    }
}

/** Round destructive icon action that sits beside a primary button (e.g. delete next to "Edit word"). */
@Composable
fun SheetDestructiveIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.size(Theme.dimensions.buttonHeight),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(Theme.dimensions.iconSizeMedium))
    }
}

@Composable
private fun ButtonLabel(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(Theme.dimensions.iconSizeMedium))
        Spacer(Modifier.size(Theme.spacing.xs))
    }
    Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}
