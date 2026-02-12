@file:OptIn(ExperimentalMaterial3Api::class)

package presentation.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.continue_reviewing
import vokab.resources.generated.resources.exit_review
import vokab.resources.generated.resources.exit_review_message
import vokab.resources.generated.resources.yes_exit

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

