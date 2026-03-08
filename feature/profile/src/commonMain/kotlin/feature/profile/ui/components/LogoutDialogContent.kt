package feature.profile.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import components.dialog.ButtonType
import components.dialog.LexiconDialogContent
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.logout
import lexicon.resources.generated.resources.logout_message
import lexicon.resources.generated.resources.logout_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun LogoutDialogContent(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    LexiconDialogContent(
        icon = Icons.AutoMirrored.Filled.Logout,
        iconTint = MaterialTheme.colorScheme.error,
        title = stringResource(Res.string.logout_title),
        message = stringResource(Res.string.logout_message),
        primaryButtonText = stringResource(Res.string.logout),
        primaryButtonOnClick = onConfirm,
        primaryButtonType = ButtonType.Error,
        secondaryButtonText = stringResource(Res.string.cancel),
        secondaryButtonOnClick = onDismiss
    )
}
