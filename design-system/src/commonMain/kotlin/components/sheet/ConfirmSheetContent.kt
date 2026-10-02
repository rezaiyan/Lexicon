package components.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import components.dialog.ContentToolbar
import theme.Theme

/**
 * Every confirmation prompt: centered icon circle, title, message, optional [extra] content,
 * a filled confirm button and a text dismiss button.
 *
 * Standalone sheets pass [onClose] for the toolbar's close button; inside
 * [overlay.bottomsheet.BottomSheetPages] the pager already draws it.
 */
@Composable
fun ConfirmSheetContent(
    icon: ImageVector,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ConfirmTone = ConfirmTone.Brand,
    confirmEnabled: Boolean = true,
    isLoading: Boolean = false,
    onClose: (() -> Unit)? = null,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth().navigationBarsPadding()) {
        ContentToolbar(onClose = onClose)
        ConfirmBody(
            icon = icon,
            title = title,
            message = message,
            confirmText = confirmText,
            onConfirm = onConfirm,
            dismissText = dismissText,
            onDismiss = onDismiss,
            tone = tone,
            confirmEnabled = confirmEnabled,
            isLoading = isLoading,
            extra = extra,
        )
    }
}

/** Same prompt as [ConfirmSheetContent], floating over whatever is on screen (e.g. above an open sheet). */
@Composable
fun ConfirmDialog(
    icon: ImageVector,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String,
    onDismiss: () -> Unit,
    tone: ConfirmTone = ConfirmTone.Brand,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(Theme.shapes.extraLarge + Theme.spacing.xxs),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.widthIn(max = Theme.dimensions.dialogMaxWidth),
        ) {
            ConfirmBody(
                icon = icon,
                title = title,
                message = message,
                confirmText = confirmText,
                onConfirm = onConfirm,
                dismissText = dismissText,
                onDismiss = onDismiss,
                tone = tone,
                modifier = Modifier.padding(Theme.spacing.lg),
            )
        }
    }
}

@Composable
private fun ConfirmBody(
    icon: ImageVector,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String,
    onDismiss: () -> Unit,
    tone: ConfirmTone,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    isLoading: Boolean = false,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val (circle, iconTint) = when (tone) {
        ConfirmTone.Brand -> colors.primary.copy(alpha = Theme.opacity.focus) to colors.primary
        ConfirmTone.Danger -> colors.errorContainer to colors.onErrorContainer
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(Theme.dimensions.iconSizeMassive)
                .clip(CircleShape)
                .background(circle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(Theme.dimensions.iconSizeLarge),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs + Theme.spacing.xxxs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = colors.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        extra?.invoke(this)
        Column(
            modifier = Modifier.padding(top = Theme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
        ) {
            if (tone == ConfirmTone.Danger) {
                SheetPrimaryButton(
                    text = confirmText,
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    isLoading = isLoading,
                    containerColor = colors.error,
                    contentColor = colors.onError,
                )
            } else {
                SheetPrimaryButton(
                    text = confirmText,
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    isLoading = isLoading,
                )
            }
            SheetTextButton(text = dismissText, onClick = onDismiss, color = colors.primary)
        }
    }
}
