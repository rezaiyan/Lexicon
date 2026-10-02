package feature.profile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import components.sheet.ConfirmSheetContent
import components.sheet.ConfirmTone
import kotlinx.coroutines.delay
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.delete_account_cooling_period_cancel
import lexicon.resources.generated.resources.delete_account_cooling_period_message
import lexicon.resources.generated.resources.delete_account_cooling_period_title
import lexicon.resources.generated.resources.delete_account_hidden_continue
import lexicon.resources.generated.resources.delete_account_hidden_message
import lexicon.resources.generated.resources.delete_account_hidden_title
import lexicon.resources.generated.resources.proceed_to_final_confirmation
import lexicon.resources.generated.resources.ready_to_proceed_deletion
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private const val CoolingPeriodSeconds = 10

@Composable
fun DeleteAccountHiddenContent(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmSheetContent(
        icon = Icons.Default.DeleteForever,
        title = stringResource(Res.string.delete_account_hidden_title),
        message = stringResource(Res.string.delete_account_hidden_message),
        confirmText = stringResource(Res.string.delete_account_hidden_continue),
        onConfirm = onConfirm,
        dismissText = stringResource(Res.string.cancel),
        onDismiss = onDismiss,
        tone = ConfirmTone.Danger,
    )
}

@Composable
fun DeleteAccountCoolingContent(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var coolingPeriodRemaining by remember { mutableIntStateOf(CoolingPeriodSeconds) }

    LaunchedEffect(Unit) {
        while (coolingPeriodRemaining > 0) {
            delay(1000)
            coolingPeriodRemaining--
        }
    }

    val coolingPeriodMessage = if (coolingPeriodRemaining > 0) {
        stringResource(Res.string.delete_account_cooling_period_message, coolingPeriodRemaining)
    } else {
        stringResource(Res.string.ready_to_proceed_deletion)
    }

    ConfirmSheetContent(
        icon = Icons.Default.Timer,
        title = stringResource(Res.string.delete_account_cooling_period_title),
        message = coolingPeriodMessage,
        confirmText = stringResource(Res.string.proceed_to_final_confirmation),
        onConfirm = onConfirm,
        dismissText = stringResource(Res.string.delete_account_cooling_period_cancel),
        onDismiss = onDismiss,
        tone = ConfirmTone.Danger,
        confirmEnabled = coolingPeriodRemaining <= 0,
        extra = {
            AnimatedVisibility(visible = coolingPeriodRemaining > 0) {
                CountdownRing(remaining = coolingPeriodRemaining, total = CoolingPeriodSeconds)
            }
        },
    )
}

@Composable
private fun CountdownRing(remaining: Int, total: Int) {
    val progress by animateFloatAsState(remaining.toFloat() / total, label = "cooling_progress")
    // The message above already announces the seconds left
    Box(
        modifier = Modifier.size(Theme.dimensions.iconSizeMassive + Theme.spacing.sm).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(Theme.dimensions.iconSizeMassive + Theme.spacing.sm),
            color = MaterialTheme.colorScheme.error,
            trackColor = MaterialTheme.colorScheme.errorContainer,
            strokeWidth = Theme.spacing.xxs + Theme.spacing.xxxs,
        )
        Text(
            text = remaining.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
