package presentation.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import components.sheet.ConfirmSheetContent
import components.sheet.SheetGroup
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import components.sheet.SheetSwitchRow
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
import lexicon.resources.generated.resources.notification_study_reminders
import lexicon.resources.generated.resources.notification_study_reminders_subtitle
import org.jetbrains.compose.resources.stringResource

@Composable
fun NotificationPermissionContent(
    onDismiss: () -> Unit,
    onEnableNotifications: () -> Unit
) {
    ConfirmSheetContent(
        icon = Icons.Default.Notifications,
        title = stringResource(Res.string.notification_permission_title),
        message = stringResource(Res.string.notification_permission_message),
        confirmText = stringResource(Res.string.notification_open_settings),
        onConfirm = onEnableNotifications,
        dismissText = stringResource(Res.string.notification_maybe_later),
        onDismiss = onDismiss,
        onClose = onDismiss,
    )
}

@Composable
fun NotificationSettingsContent(
    notificationsEnabled: Boolean,
    systemNotificationsEnabled: Boolean,
    reviewRemindersEnabled: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    onReviewRemindersToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    SheetPage(
        title = stringResource(Res.string.notification_settings_title),
        subtitle = stringResource(Res.string.notification_settings_subtitle),
        onClose = onDismiss,
        footer = {
            SheetPrimaryButton(text = stringResource(Res.string.done), onClick = onDismiss)
        },
    ) {
        SheetGroup {
            SheetSwitchRow(
                title = stringResource(Res.string.notification_enable_notifications),
                subtitle = if (notificationsEnabled) {
                    stringResource(Res.string.notification_stay_motivated)
                } else {
                    stringResource(Res.string.notification_missing_nudges)
                },
                icon = Icons.Default.Notifications,
                checked = notificationsEnabled,
                onCheckedChange = onNotificationsToggle,
                enabled = systemNotificationsEnabled,
                showDivider = true,
            )
            SheetSwitchRow(
                title = stringResource(Res.string.notification_study_reminders),
                subtitle = stringResource(Res.string.notification_study_reminders_subtitle),
                icon = Icons.Default.Schedule,
                checked = reviewRemindersEnabled,
                onCheckedChange = onReviewRemindersToggle,
                enabled = notificationsEnabled,
                showDivider = false,
            )
        }
        if (notificationsEnabled) {
            Text(
                stringResource(Res.string.notification_gentle_reminders),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
