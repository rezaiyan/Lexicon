package presentation.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.SettingsCard
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.notification_on_alerts_enabled
import lexicon.resources.generated.resources.notification_disabled
import lexicon.resources.generated.resources.notification_permission_required
import lexicon.resources.generated.resources.notifications

private val NotificationIconColor = Color(0xFF5C6BC0)

@Composable
fun NotificationSettingsCard(
    systemNotificationsEnabled: Boolean,
    notificationsEnabled: Boolean,
    onEnable: () -> Unit
) {
    val isError = !systemNotificationsEnabled

    SettingsCard(
        icon = Icons.Default.Notifications,
        title = stringResource(Res.string.notifications),
        subtitle = when {
            !systemNotificationsEnabled -> stringResource(Res.string.notification_permission_required)
            notificationsEnabled -> stringResource(Res.string.notification_on_alerts_enabled)
            else -> stringResource(Res.string.notification_disabled)
        },
        onClick = onEnable,
        iconTint = if (isError) MaterialTheme.colorScheme.error else null,
        iconBackgroundColor = if (isError) MaterialTheme.colorScheme.error else NotificationIconColor,
        subtitleColor = if (isError) MaterialTheme.colorScheme.error else null
    )
}
