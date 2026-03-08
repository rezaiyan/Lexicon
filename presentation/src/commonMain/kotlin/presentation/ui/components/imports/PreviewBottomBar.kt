package presentation.ui.components.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import components.dialog.ButtonState
import components.dialog.ButtonType
import components.dialog.LexiconDialogContent
import theme.AppDimensions
import theme.AppSpacing
import theme.Theme

@Composable
internal fun PreviewBottomBar(
    isLoading: Boolean,
    selectedCount: Int,
    onImport: () -> Unit,
    spacing: AppSpacing,
    dimensions: AppDimensions,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = Theme.elevation.overlay,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(spacing.md))
            if (isLoading) {
                Row(
                    modifier = Modifier.padding(vertical = spacing.small),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(spacing.extraSmall2))
                    Text(
                        text = "Adding to your library…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Button(
                    onClick = onImport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = Theme.dimensions.contentMaxWidth),
                    enabled = selectedCount > 0,
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = Theme.spacing.lg),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(dimensions.iconSizeMedium)
                    )
                    Spacer(modifier = Modifier.size(spacing.extraSmall2))
                    Text(
                        text = if (selectedCount > 0) "Add $selectedCount Words to Library"
                        else "Select words to add",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Spacer(modifier = Modifier.height(spacing.md))
        }
    }
}

@Composable
internal fun DiscardConfirmationContent(
    onDiscard: () -> Unit,
    onKeep: () -> Unit,
) {
    LexiconDialogContent(
        title = "Discard suggestions?",
        message = "Your AI-generated vocabulary list will be lost.",
        primaryButton = ButtonState(
            text = "Discard",
            onClick = onDiscard,
            type = ButtonType.Error
        ),
        secondaryButton = ButtonState(
            text = "Keep",
            onClick = onKeep
        ),
    )
}
