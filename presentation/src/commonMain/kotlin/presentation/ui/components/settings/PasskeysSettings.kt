package presentation.ui.components.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import components.sheet.ConfirmSheetContent
import components.sheet.ConfirmTone
import components.sheet.SheetGroup
import components.sheet.SheetOptionRow
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import domain.common.util.EpochDateFormatter
import domain.passkey.model.Passkey
import feature.settings.PasskeysViewModel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.passkey_create
import lexicon.resources.generated.resources.passkey_created_on
import lexicon.resources.generated.resources.passkey_delete
import lexicon.resources.generated.resources.passkey_delete_message
import lexicon.resources.generated.resources.passkey_delete_title
import lexicon.resources.generated.resources.passkey_deleting
import lexicon.resources.generated.resources.passkey_last_used
import lexicon.resources.generated.resources.passkey_synced
import lexicon.resources.generated.resources.passkey_title
import lexicon.resources.generated.resources.passkeys
import lexicon.resources.generated.resources.passkeys_empty
import lexicon.resources.generated.resources.passkeys_sheet_subtitle
import lexicon.resources.generated.resources.passkeys_subtitle
import org.jetbrains.compose.resources.stringResource
import overlay.OverlayHost
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import overlay.bottomsheet.showSizeToFitBottomSheet
import presentation.ui.components.SettingsCard
import theme.AppColors

/** Settings entry for passkeys. Only shown on devices that can create one. */
@Composable
fun PasskeysSettingsCard(onClick: () -> Unit) {
    SettingsCard(
        icon = Icons.Rounded.Key,
        title = stringResource(Res.string.passkeys),
        subtitle = stringResource(Res.string.passkeys_subtitle),
        iconBackgroundColor = AppColors.settingsPasskeysIcon,
        onClick = onClick,
    )
}

fun OverlayHost.showPasskeysSheet(viewModel: PasskeysViewModel) {
    viewModel.refresh()
    showSizeToFitBottomSheet(tag = "passkeys") { nav ->
        val state by viewModel.state()
        val pages = rememberBottomSheetPageNavigator<PasskeysPage>(PasskeysPage.List)
        BottomSheetPages(navigator = pages, onClose = { nav.dismiss() }, label = "passkeysPages") { page ->
            when (page) {
                PasskeysPage.List -> PasskeysContent(
                    passkeys = state.passkeys,
                    creating = state.creating,
                    deletingId = state.deletingId,
                    error = state.error,
                    onCreate = viewModel::create,
                    onPasskeyClick = { passkey -> pages.navigateTo(PasskeysPage.ConfirmDelete(passkey.id)) },
                )

                is PasskeysPage.ConfirmDelete -> ConfirmSheetContent(
                    icon = Icons.Rounded.Delete,
                    title = stringResource(Res.string.passkey_delete_title),
                    message = stringResource(Res.string.passkey_delete_message),
                    confirmText = stringResource(Res.string.passkey_delete),
                    onConfirm = {
                        viewModel.delete(page.id)
                        pages.navigateBack()
                    },
                    dismissText = stringResource(Res.string.cancel),
                    onDismiss = { pages.navigateBack() },
                    tone = ConfirmTone.Danger,
                )
            }
        }
    }
}

@Composable
private fun PasskeysContent(
    passkeys: List<Passkey>,
    creating: Boolean,
    deletingId: Long?,
    error: String?,
    onCreate: () -> Unit,
    onPasskeyClick: (Passkey) -> Unit,
) {
    SheetPage(
        title = stringResource(Res.string.passkeys),
        subtitle = stringResource(Res.string.passkeys_sheet_subtitle),
        footer = {
            SheetPrimaryButton(
                text = stringResource(Res.string.passkey_create),
                onClick = onCreate,
                isLoading = creating,
                enabled = !creating && deletingId == null,
                icon = Icons.Rounded.Key,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (passkeys.isEmpty()) {
            Text(
                text = stringResource(Res.string.passkeys_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            SheetGroup {
                passkeys.forEachIndexed { index, passkey ->
                    val deleting = passkey.id == deletingId
                    SheetOptionRow(
                        icon = Icons.Rounded.Key,
                        title = stringResource(Res.string.passkey_title),
                        subtitle = if (deleting) stringResource(Res.string.passkey_deleting) else passkey.details(),
                        onClick = { if (deletingId == null && !creating) onPasskeyClick(passkey) },
                        showDivider = index < passkeys.lastIndex,
                        showChevron = !deleting,
                        trailingContent = if (deleting) {
                            { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Passkey.details(): String = listOfNotNull(
    lastUsedAtMillis?.let { stringResource(Res.string.passkey_last_used, EpochDateFormatter.toMediumDate(it)) }
        ?: stringResource(Res.string.passkey_created_on, EpochDateFormatter.toMediumDate(createdAtMillis)),
    if (synced) stringResource(Res.string.passkey_synced) else null,
).joinToString(" · ")

private sealed interface PasskeysPage {
    data object List : PasskeysPage
    data class ConfirmDelete(val id: Long) : PasskeysPage
}
