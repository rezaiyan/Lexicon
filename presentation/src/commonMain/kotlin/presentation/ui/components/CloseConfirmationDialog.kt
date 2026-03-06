@file:OptIn(ExperimentalMaterial3Api::class)

package presentation.ui.components

import components.dialog.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.continue_reviewing
import lexicon.resources.generated.resources.exit_review
import lexicon.resources.generated.resources.exit_review_message
import lexicon.resources.generated.resources.yes_exit

@Composable
fun CloseConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.exit_review),
        message = stringResource(Res.string.exit_review_message),
        primaryButtonText = stringResource(Res.string.yes_exit),
        primaryButtonOnClick = onConfirm,
        secondaryButtonText = stringResource(Res.string.continue_reviewing),
        secondaryButtonOnClick = onDismiss
    )
}

