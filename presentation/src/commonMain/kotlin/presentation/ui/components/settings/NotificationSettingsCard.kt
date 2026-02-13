package presentation.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.SettingsCard
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.notification_disabled
import lexicon.resources.generated.resources.notification_enabled
import lexicon.resources.generated.resources.notification_permission_required
import lexicon.resources.generated.resources.notifications

@Composable
fun NotificationSettingsCard(
    systemNotificationsEnabled: Boolean,
    notificationsEnabled: Boolean,
    onEnable: () -> Unit
) {
    SettingsCard(
        icon = Icons.Default.Notifications,
        title = stringResource(Res.string.notifications),
        subtitle = when {
            !systemNotificationsEnabled -> stringResource(Res.string.notification_permission_required)
            notificationsEnabled -> stringResource(Res.string.notification_enabled)
            else -> stringResource(Res.string.notification_disabled)
        },
        onClick = onEnable,
        iconTint = if (!systemNotificationsEnabled) MaterialTheme.colorScheme.error else null,
        subtitleColor = if (!systemNotificationsEnabled) MaterialTheme.colorScheme.error else null
    )
}



