package presentation.ui.components.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import components.GroupedRow
import components.GroupedSection
import domain.connectivity.model.ServerStatus
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.server_status
import lexicon.resources.generated.resources.server_status_checking
import lexicon.resources.generated.resources.server_status_offline
import lexicon.resources.generated.resources.server_status_online
import lexicon.resources.generated.resources.server_status_retry
import lexicon.resources.generated.resources.server_status_unavailable
import lexicon.resources.generated.resources.server_status_works_offline
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/**
 * One quiet row saying whether Lexicon's server can be reached. Degraded states say what still works
 * (everything on the device) and tapping re-checks; while online it's a plain confirmation.
 */
@Composable
fun ServerStatusSection(
    status: ServerStatus,
    retrying: Boolean,
    onRetry: () -> Unit,
) {
    val look = status.look()
    val checking = retrying || status == ServerStatus.Checking
    GroupedSection(title = stringResource(Res.string.server_status)) {
        GroupedRow(
            title = stringResource(look.title),
            subtitle = look.detail?.let { stringResource(it) },
            onClick = onRetry,
            modifier = Modifier
                .animateContentSize()
                // Screen readers hear the change when the server goes down or comes back
                .semantics { liveRegion = LiveRegionMode.Polite },
            icon = look.icon,
            iconColor = look.tint(),
            showChevron = false,
            showDivider = false,
            trailingContent = when {
                checking -> {
                    {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
                            strokeWidth = 2.dp,
                        )
                    }
                }
                look.degraded -> {
                    {
                        Text(
                            text = stringResource(Res.string.server_status_retry),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                else -> null
            },
        )
    }
}

private class StatusLook(
    val title: StringResource,
    val detail: StringResource?,
    val icon: ImageVector,
    val degraded: Boolean,
    val tint: @Composable () -> Color,
)

private fun ServerStatus.look(): StatusLook = when (this) {
    ServerStatus.Online -> StatusLook(
        title = Res.string.server_status_online,
        detail = null,
        icon = Icons.Rounded.CloudDone,
        degraded = false,
        tint = { Theme.colors.success },
    )
    ServerStatus.Checking -> StatusLook(
        title = Res.string.server_status_checking,
        detail = null,
        icon = Icons.Rounded.CloudSync,
        degraded = false,
        tint = { MaterialTheme.colorScheme.onSurfaceVariant },
    )
    ServerStatus.Offline -> StatusLook(
        title = Res.string.server_status_offline,
        detail = Res.string.server_status_works_offline,
        icon = Icons.Rounded.WifiOff,
        degraded = true,
        tint = { Theme.colors.warning },
    )
    ServerStatus.Unavailable -> StatusLook(
        title = Res.string.server_status_unavailable,
        detail = Res.string.server_status_works_offline,
        icon = Icons.Rounded.CloudOff,
        degraded = true,
        tint = { Theme.colors.warning },
    )
}
