package presentation.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import domain.connectivity.model.ServerStatus
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.server_status_offline
import lexicon.resources.generated.resources.server_status_retry
import lexicon.resources.generated.resources.server_status_unavailable
import lexicon.resources.generated.resources.server_status_works_offline
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/**
 * Slim notice shown only while the server can't be reached; nothing at all while connected or
 * still checking. Says what keeps working and offers a re-check.
 */
@Composable
fun ServerStatusNotice(
    status: ServerStatus,
    retrying: Boolean,
    onRetry: () -> Unit,
) {
    val offline = status == ServerStatus.Offline
    AnimatedVisibility(
        visible = offline || status == ServerStatus.Unavailable,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Theme.shapes.medium))
                .background(Theme.colors.warningContainer)
                // Screen readers hear it when the server goes down or comes back
                .semantics { liveRegion = LiveRegionMode.Polite }
                .padding(start = Theme.spacing.cardPadding, end = Theme.spacing.xxs)
                .padding(vertical = Theme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.inlineGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (offline) Icons.Rounded.WifiOff else Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = Theme.colors.warning,
                modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (offline) Res.string.server_status_offline else Res.string.server_status_unavailable,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = Theme.colors.onWarningContainer,
                )
                Text(
                    text = stringResource(Res.string.server_status_works_offline),
                    style = MaterialTheme.typography.bodySmall,
                    color = Theme.colors.onWarningContainer.copy(alpha = 0.8f),
                )
            }
            if (retrying) {
                Box(modifier = Modifier.size(Theme.dimensions.touchTargetSmall), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
                        strokeWidth = 2.dp,
                        color = Theme.colors.onWarningContainer,
                    )
                }
            } else {
                TextButton(onClick = onRetry) {
                    Text(text = stringResource(Res.string.server_status_retry))
                }
            }
        }
    }
}
