package presentation.ui.components.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import components.dialog.ButtonState
import components.dialog.ButtonType
import components.dialog.LexiconDialogContent
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.ai_wizard_add_to_library
import lexicon.resources.generated.resources.ai_wizard_adding_to_library
import lexicon.resources.generated.resources.ai_wizard_discard
import lexicon.resources.generated.resources.ai_wizard_discard_message
import lexicon.resources.generated.resources.ai_wizard_discard_title
import lexicon.resources.generated.resources.ai_wizard_keep
import org.jetbrains.compose.resources.stringResource
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            Row(
                modifier = Modifier.padding(vertical = spacing.md),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                Text(
                    text = stringResource(Res.string.ai_wizard_adding_to_library),
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
                contentPadding = PaddingValues(vertical = spacing.sm, horizontal = spacing.lg),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(Theme.shapes.pill)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(dimensions.iconSizeMedium)
                )
                Spacer(modifier = Modifier.size(spacing.xs))
                Text(
                    text = stringResource(Res.string.ai_wizard_add_to_library),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
internal fun DiscardConfirmationContent(
    onDiscard: () -> Unit,
    onKeep: () -> Unit,
) {
    LexiconDialogContent(
        title = stringResource(Res.string.ai_wizard_discard_title),
        message = stringResource(Res.string.ai_wizard_discard_message),
        primaryButton = ButtonState(
            text = stringResource(Res.string.ai_wizard_discard),
            onClick = onDiscard,
            type = ButtonType.Error
        ),
        secondaryButton = ButtonState(
            text = stringResource(Res.string.ai_wizard_keep),
            onClick = onKeep
        ),
    )
}
