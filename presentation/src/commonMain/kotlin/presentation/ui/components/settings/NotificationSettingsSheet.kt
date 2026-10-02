package presentation.ui.components.settings

import androidx.compose.runtime.getValue
import feature.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel
import overlay.OverlayHost
import overlay.bottomsheet.showSizeToFitBottomSheet
import presentation.ui.components.NotificationPermissionContent
import presentation.ui.components.NotificationSettingsContent
import presentation.ui.permissions.rememberNotificationPermissionRequester
import presentation.ui.permissions.wasNotificationPermissionDenied

/**
 * Notification preferences when the OS allows notifications, otherwise the permission prompt.
 * Shared by the Settings screen (passes its own [settingsViewModel]) and the Insights reminder
 * entry point (resolves one inside the sheet).
 */
fun OverlayHost.showNotificationSettingsSheet(settingsViewModel: SettingsViewModel? = null) {
    showSizeToFitBottomSheet(tag = "notification-settings") { nav ->
        val viewModel = settingsViewModel ?: koinViewModel<SettingsViewModel>()
        val currentState by viewModel.state()
        val screen = currentState.screen
        if (screen.systemNotificationsEnabled) {
            NotificationSettingsContent(
                notificationsEnabled = screen.notificationsEnabled,
                systemNotificationsEnabled = screen.systemNotificationsEnabled,
                reviewRemindersEnabled = screen.reviewRemindersEnabled,
                onNotificationsToggle = { viewModel.setNotificationsEnabled(it) },
                onReviewRemindersToggle = { viewModel.setReviewRemindersEnabled(it) },
                onDismiss = { nav.dismiss() },
            )
        } else {
            val deniedPreviously = wasNotificationPermissionDenied()
            val requestPermission = rememberNotificationPermissionRequester { granted ->
                if (granted) viewModel.setNotificationsEnabled(true)
                viewModel.refreshNotificationPermissionStatus()
                nav.dismiss()
            }
            NotificationPermissionContent(
                onDismiss = { nav.dismiss() },
                onEnableNotifications = {
                    if (deniedPreviously) {
                        nav.dismiss()
                        viewModel.requestNotificationPermission()
                        viewModel.refreshNotificationPermissionStatus()
                    } else {
                        requestPermission()
                    }
                },
            )
        }
    }
}
