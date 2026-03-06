package presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.feature.settings.SettingsViewModel
import presentation.model.DialogState
import presentation.ui.components.LanguageSelectionDialog
import components.scaffold.LexiconColumn
import presentation.ui.components.NotificationPermissionDialog
import presentation.ui.components.NotificationSettingsDialog
import presentation.ui.components.ThemeModeDialog
import presentation.ui.components.settings.AboutSettingsCard
import presentation.ui.components.settings.LanguageSettingsCard
import presentation.ui.components.settings.NotificationSettingsCard
import presentation.ui.components.settings.SubscriptionCard
import presentation.ui.components.settings.ThemeSettingsCard
import presentation.ui.components.settings.WordManagerCard
import presentation.ui.permissions.rememberNotificationPermissionRequester
import presentation.ui.permissions.wasNotificationPermissionDenied
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.settings

@Composable
fun SettingsScreen(
    onNavigateToWordManager: () -> Unit,
    onNavigateToSubscription: () -> Unit = {},
) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val settingsState by viewModel.state()
    val state = settingsState.screen
    val dialogState = settingsState.dialog
    val currentLanguage = state.currentLanguage
    val themeMode = state.themeMode
    val notificationsEnabled = state.notificationsEnabled
    val systemNotificationsEnabled = state.systemNotificationsEnabled

    LexiconColumn(
        title = stringResource(Res.string.settings),
        scrollable = true,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacingLarge)
        ) {
            if (state.isPremiumFeatureEnabled) {
                LanguageSettingsCard(
                    currentLanguage = currentLanguage,
                    onShowLanguageDialog = { viewModel.showDialog(DialogState.LanguageSelection) }
                )
            }

            ThemeSettingsCard(
                themeMode = themeMode,
                onShowThemeDialog = { viewModel.showDialog(DialogState.ThemeSelection) }
            )

            NotificationSettingsCard(
                systemNotificationsEnabled = systemNotificationsEnabled,
                notificationsEnabled = notificationsEnabled,
                onEnable = {
                    if (systemNotificationsEnabled) {
                        viewModel.showDialog(DialogState.NotificationSettings)
                    } else {
                        viewModel.showDialog(DialogState.NotificationPermission)
                    }
                }
            )

            WordManagerCard(onClick = onNavigateToWordManager)

            SubscriptionCard(onClick = onNavigateToSubscription)

            AboutSettingsCard(appVersion = state.appVersion)
        }

    }

    if (dialogState is DialogState.LanguageSelection) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onDismiss = { viewModel.dismissDialog() },
            onLanguageSelected = { language ->
                viewModel.setLanguage(language)
                viewModel.dismissDialog()
            }
        )
    }

    if (dialogState is DialogState.ThemeSelection) {
        ThemeModeDialog(
            currentThemeMode = themeMode,
            onDismiss = { viewModel.dismissDialog() },
            onThemeModeSelected = { mode ->
                viewModel.setThemeMode(mode)
                viewModel.dismissDialog()
            }
        )
    }

    if (dialogState is DialogState.NotificationPermission) {
        val deniedPreviously = wasNotificationPermissionDenied()
        val requestPermission = rememberNotificationPermissionRequester { granted ->
            if (granted) {
                viewModel.setNotificationsEnabled(true)
                viewModel.dismissDialog()
                viewModel.refreshNotificationPermissionStatus()
            } else {
                viewModel.dismissDialog()
                viewModel.refreshNotificationPermissionStatus()
            }
        }
        NotificationPermissionDialog(
            onDismiss = { viewModel.dismissDialog() },
            onEnableNotifications = {
                if (deniedPreviously) {
                    viewModel.dismissDialog()
                    viewModel.requestNotificationPermission()
                    viewModel.refreshNotificationPermissionStatus()
                } else {
                    requestPermission()
                }
            }
        )
    }

    if (dialogState is DialogState.NotificationSettings) {
        NotificationSettingsDialog(
            notificationsEnabled = notificationsEnabled,
            systemNotificationsEnabled = systemNotificationsEnabled,
            onNotificationsToggle = { viewModel.setNotificationsEnabled(it) },
            onDismiss = { viewModel.dismissDialog() }
        )
    }
}
