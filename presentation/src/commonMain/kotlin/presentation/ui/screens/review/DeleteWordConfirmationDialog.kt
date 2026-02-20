@file:OptIn(ExperimentalMaterial3Api::class)

package presentation.ui.screens.review

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import domain.word.model.Word
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.BasicAlertDialog
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.delete

@Composable
fun DeleteWordConfirmationDialog(
    word: Word,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Default.Warning,
        iconTint = MaterialTheme.colorScheme.error,
        title = "Delete word?",
        message = "\"${word.originalWord}\" will be permanently removed. This cannot be undone.",
        // Cancel is the safe TextButton action (end-aligned)
        secondaryButtonText = stringResource(Res.string.cancel),
        secondaryButtonOnClick = onDismiss,
        // Delete is the destructive full-width outlined error button
        negativeButtonText = stringResource(Res.string.delete),
        negativeButtonOnClick = onConfirm
    )
}
