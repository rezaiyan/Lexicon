package presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import components.dialog.ButtonState
import components.dialog.DialogIconState
import components.dialog.LexiconDialogContent
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.done
import lexicon.resources.generated.resources.notification_enable_notifications
import lexicon.resources.generated.resources.notification_gentle_reminders
import lexicon.resources.generated.resources.notification_maybe_later
import lexicon.resources.generated.resources.notification_missing_nudges
import lexicon.resources.generated.resources.notification_open_settings
import lexicon.resources.generated.resources.notification_permission_message
import lexicon.resources.generated.resources.notification_permission_title
import lexicon.resources.generated.resources.notification_settings_subtitle
import lexicon.resources.generated.resources.notification_settings_title
import lexicon.resources.generated.resources.notification_stay_motivated

@Composable
fun NotificationPermissionContent(
    onDismiss: () -> Unit,
    onEnableNotifications: () -> Unit
) {
    LexiconDialogContent(
        icon = Icons.Default.Notifications,
        title = stringResource(Res.string.notification_permission_title),
        message = stringResource(Res.string.notification_permission_message),
        primaryButtonText = stringResource(Res.string.notification_open_settings),
        primaryButtonOnClick = onEnableNotifications,
        secondaryButtonText = stringResource(Res.string.notification_maybe_later),
        secondaryButtonOnClick = onDismiss
    )
}

@Composable
fun NotificationSettingsContent(
    notificationsEnabled: Boolean,
    systemNotificationsEnabled: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    LexiconDialogContent(
        iconState = DialogIconState.Icon(Icons.Default.Notifications),
        title = stringResource(Res.string.notification_settings_title),
        content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacingLarge)
                ) {
                    Text(
                        stringResource(Res.string.notification_settings_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Theme.spacing.cardPadding),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(Res.string.notification_enable_notifications),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    if (notificationsEnabled) stringResource(Res.string.notification_stay_motivated)
                                    else stringResource(Res.string.notification_missing_nudges),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = if (systemNotificationsEnabled) onNotificationsToggle else null,
                                enabled = systemNotificationsEnabled
                            )
                        }
                    }

                    if (notificationsEnabled) {
                        Text(
                            stringResource(Res.string.notification_gentle_reminders),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            primaryButton = ButtonState(
                text = stringResource(Res.string.done),
                onClick = onDismiss
            )
        )
}
