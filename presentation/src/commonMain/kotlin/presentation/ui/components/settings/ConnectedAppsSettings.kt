package presentation.ui.components.settings

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.SmartToy
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
import domain.connectedapps.model.ConnectedApp
import domain.connectedapps.model.ConnectedAppAccess
import feature.settings.ConnectedAppsViewModel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.connected_app_access_add_words
import lexicon.resources.generated.resources.connected_app_access_delete_words
import lexicon.resources.generated.resources.connected_app_access_read_progress
import lexicon.resources.generated.resources.connected_app_access_read_words
import lexicon.resources.generated.resources.connected_app_disconnect
import lexicon.resources.generated.resources.connected_app_disconnect_failed
import lexicon.resources.generated.resources.connected_app_disconnect_message
import lexicon.resources.generated.resources.connected_app_disconnect_title
import lexicon.resources.generated.resources.connected_app_disconnecting
import lexicon.resources.generated.resources.connected_apps
import lexicon.resources.generated.resources.connected_apps_empty
import lexicon.resources.generated.resources.connected_apps_sheet_subtitle
import lexicon.resources.generated.resources.connected_apps_subtitle
import org.jetbrains.compose.resources.stringResource
import overlay.OverlayHost
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import overlay.bottomsheet.showSizeToFitBottomSheet
import presentation.ui.components.SettingsCard
import theme.AppColors

/** Settings entry for AI apps connected over MCP. Only shown while at least one is connected. */
@Composable
fun ConnectedAppsSettingsCard(onClick: () -> Unit) {
    SettingsCard(
        icon = Icons.Rounded.SmartToy,
        title = stringResource(Res.string.connected_apps),
        subtitle = stringResource(Res.string.connected_apps_subtitle),
        iconBackgroundColor = AppColors.settingsConnectedAppsIcon,
        onClick = onClick,
    )
}

fun OverlayHost.showConnectedAppsSheet(viewModel: ConnectedAppsViewModel) {
    viewModel.refresh()
    showSizeToFitBottomSheet(tag = "connected-apps") { nav ->
        val state by viewModel.state()
        val pages = rememberBottomSheetPageNavigator<ConnectedAppsPage>(ConnectedAppsPage.List)
        BottomSheetPages(navigator = pages, onClose = { nav.dismiss() }, label = "connectedAppsPages") { page ->
            when (page) {
                ConnectedAppsPage.List -> ConnectedAppsContent(
                    apps = state.apps,
                    disconnectingId = state.disconnectingId,
                    failedId = state.failedId,
                    onAppClick = { app -> pages.navigateTo(ConnectedAppsPage.Confirm(app.id, app.name)) },
                )

                is ConnectedAppsPage.Confirm -> ConfirmSheetContent(
                    icon = Icons.Rounded.LinkOff,
                    title = stringResource(Res.string.connected_app_disconnect_title, page.name),
                    message = stringResource(Res.string.connected_app_disconnect_message, page.name),
                    confirmText = stringResource(Res.string.connected_app_disconnect),
                    onConfirm = {
                        viewModel.disconnect(page.id)
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
private fun ConnectedAppsContent(
    apps: List<ConnectedApp>,
    disconnectingId: String?,
    failedId: String?,
    onAppClick: (ConnectedApp) -> Unit,
) {
    SheetPage(
        title = stringResource(Res.string.connected_apps),
        subtitle = stringResource(Res.string.connected_apps_sheet_subtitle),
    ) {
        if (apps.isEmpty()) {
            Text(
                text = stringResource(Res.string.connected_apps_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            SheetGroup {
                apps.forEachIndexed { index, app ->
                    val disconnecting = app.id == disconnectingId
                    SheetOptionRow(
                        icon = Icons.Rounded.SmartToy,
                        title = app.name,
                        subtitle = when {
                            disconnecting -> stringResource(Res.string.connected_app_disconnecting)
                            app.id == failedId -> stringResource(Res.string.connected_app_disconnect_failed)
                            else -> listOfNotNull(app.website, app.access.label()).joinToString(" · ")
                                .ifEmpty { null }
                        },
                        onClick = { if (disconnectingId == null) onAppClick(app) },
                        showDivider = index < apps.lastIndex,
                        showChevron = !disconnecting,
                        trailingContent = if (disconnecting) {
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
private fun Set<ConnectedAppAccess>.label(): String? {
    val labels = ConnectedAppAccess.entries.filter { it in this }.map { access ->
        when (access) {
            ConnectedAppAccess.READ_WORDS -> stringResource(Res.string.connected_app_access_read_words)
            ConnectedAppAccess.ADD_WORDS -> stringResource(Res.string.connected_app_access_add_words)
            ConnectedAppAccess.DELETE_WORDS -> stringResource(Res.string.connected_app_access_delete_words)
            ConnectedAppAccess.READ_PROGRESS -> stringResource(Res.string.connected_app_access_read_progress)
        }
    }
    return labels.joinToString(" · ").ifEmpty { null }
}

private sealed interface ConnectedAppsPage {
    data object List : ConnectedAppsPage
    data class Confirm(val id: String, val name: String) : ConnectedAppsPage
}
