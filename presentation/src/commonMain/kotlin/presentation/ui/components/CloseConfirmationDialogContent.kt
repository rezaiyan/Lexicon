package presentation.ui.components

import components.dialog.LexiconDialogContent
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.continue_reviewing
import lexicon.resources.generated.resources.exit_review
import lexicon.resources.generated.resources.exit_review_message
import lexicon.resources.generated.resources.yes_exit

@Composable
fun CloseConfirmationDialogContent(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    LexiconDialogContent(
        title = stringResource(Res.string.exit_review),
        message = stringResource(Res.string.exit_review_message),
        primaryButtonText = stringResource(Res.string.yes_exit),
        primaryButtonOnClick = onConfirm,
        secondaryButtonText = stringResource(Res.string.continue_reviewing),
        secondaryButtonOnClick = onDismiss
    )
}

