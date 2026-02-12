package presentation.ui.components.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.ButtonType
import presentation.ui.components.VokabDialogContent
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.cancel
import vokab.resources.generated.resources.logout
import vokab.resources.generated.resources.logout_message
import vokab.resources.generated.resources.logout_title

@Composable
fun LogoutDialogContent(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    VokabDialogContent(
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

