package presentation.ui.components.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.ButtonType
import presentation.ui.components.LexiconDialogContent
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.logout
import lexicon.resources.generated.resources.logout_message
import lexicon.resources.generated.resources.logout_title

@Composable
fun LogoutDialogContent(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    LexiconDialogContent(
        icon = Icons.AutoMirrored.Filled.Logout,
        title = stringResource(Res.string.logout_title),
        message = stringResource(Res.string.logout_message),
        primaryButtonText = stringResource(Res.string.logout),
        primaryButtonOnClick = onConfirm,
        primaryButtonType = ButtonType.Error,
        secondaryButtonText = stringResource(Res.string.cancel),
        secondaryButtonOnClick = onDismiss
    )
}

