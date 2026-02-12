package presentation.ui.components.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.ButtonState
import presentation.ui.components.ButtonType
import presentation.ui.components.DialogIconState
import presentation.ui.components.DialogProgressState
import presentation.ui.components.VokabDialogContent
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.cancel
import vokab.resources.generated.resources.delete_account_cooling_period_cancel
import vokab.resources.generated.resources.delete_account_cooling_period_message
import vokab.resources.generated.resources.delete_account_cooling_period_title
import vokab.resources.generated.resources.delete_account_hidden_continue
import vokab.resources.generated.resources.delete_account_hidden_message
import vokab.resources.generated.resources.delete_account_hidden_title
import vokab.resources.generated.resources.proceed_to_final_confirmation
import vokab.resources.generated.resources.ready_to_proceed_deletion

@Composable
fun DeleteAccountHiddenDialogContent(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    VokabDialogContent(
        icon = Icons.Default.Delete,
        iconTint = MaterialTheme.colorScheme.error,
        title = stringResource(Res.string.delete_account_hidden_title),
        message = stringResource(Res.string.delete_account_hidden_message),
        primaryButtonText = stringResource(Res.string.delete_account_hidden_continue),
        primaryButtonOnClick = onConfirm,
        primaryButtonType = ButtonType.Error,
        secondaryButtonText = stringResource(Res.string.cancel),
        secondaryButtonOnClick = onDismiss
    )
}

@Composable
fun DeleteAccountCoolingDialogContent(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var coolingPeriodRemaining by remember { mutableStateOf(10) }

    LaunchedEffect(Unit) {
        while (coolingPeriodRemaining > 0) {
            delay(1000)
            coolingPeriodRemaining--
        }
    }

    val coolingPeriodMessage = if (coolingPeriodRemaining > 0) {
        stringResource(Res.string.delete_account_cooling_period_message).replace("%1\$d", coolingPeriodRemaining.toString())
    } else {
        stringResource(Res.string.ready_to_proceed_deletion)
    }
    
    VokabDialogContent(
        iconState = DialogIconState.Icon(Icons.Default.Timer),
        title = stringResource(Res.string.delete_account_cooling_period_title),
        message = coolingPeriodMessage,
        progressState = if (coolingPeriodRemaining > 0) {
            DialogProgressState.Circular
        } else {
            DialogProgressState.None
        },
        primaryButton = ButtonState(
            text = stringResource(Res.string.proceed_to_final_confirmation),
            onClick = onConfirm,
            enabled = coolingPeriodRemaining <= 0,
            type = ButtonType.Error
        ),
        secondaryButton = ButtonState(
            text = stringResource(Res.string.delete_account_cooling_period_cancel),
            onClick = onDismiss
        )
    )
}

