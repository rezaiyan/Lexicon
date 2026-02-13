package presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.feature.settings.SettingsViewModel
import presentation.feature.settings.model.SettingsEvent
import presentation.model.DialogState
import presentation.model.SettingsScreenState
import presentation.ui.components.LanguageSelectionDialog
import presentation.ui.components.LexiconColumn
import presentation.ui.components.NotificationPermissionDialog
import presentation.ui.components.NotificationSettingsDialog
import presentation.ui.components.ThemeModeDialog
import presentation.ui.components.settings.AboutSettingsCard
import presentation.ui.components.settings.LanguageSettingsCard
import presentation.ui.components.settings.NotificationSettingsCard
import presentation.ui.components.settings.ReviewSettingsCard
import presentation.ui.components.settings.ReviewSettingsDialog
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
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val state by settingsViewModel.settingsScreenState.collectAsStateWithLifecycle()
    val dialogState by settingsViewModel.dialogState.collectAsStateWithLifecycle()

    SettingsScreenContent(
        state = state,
        dialogState = dialogState,
        onEvent = settingsViewModel::onEvent,
        onNavigateToWordManager = onNavigateToWordManager,
        onNavigateToSubscription = onNavigateToSubscription,
    )
}

@Composable
private fun SettingsScreenContent(
    state: SettingsScreenState,
    dialogState: DialogState,
    onEvent: (SettingsEvent) -> Unit,
    onNavigateToWordManager: () -> Unit,
    onNavigateToSubscription: () -> Unit,
) {
    val currentLanguage = state.currentLanguage
    val themeMode = state.themeMode
    val notificationsEnabled = state.notificationsEnabled
    val systemNotificationsEnabled = state.systemNotificationsEnabled

    LexiconColumn(
        title = stringResource(Res.string.settings),
        scrollable = true,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing)
        ) {
            if (state.isPremiumFeatureEnabled) {
                LanguageSettingsCard(
                    currentLanguage = currentLanguage,
                    onShowLanguageDialog = { onEvent(SettingsEvent.ShowDialog(DialogState.LanguageSelection)) }
                )
            }

            ThemeSettingsCard(
                themeMode = themeMode,
                onShowThemeDialog = { onEvent(SettingsEvent.ShowDialog(DialogState.ThemeSelection)) }
            )

            NotificationSettingsCard(
                systemNotificationsEnabled = systemNotificationsEnabled,
                notificationsEnabled = notificationsEnabled,
                onEnable = {
                    if (systemNotificationsEnabled) {
                        onEvent(SettingsEvent.ShowDialog(DialogState.NotificationSettings))
                    } else {
                        onEvent(SettingsEvent.ShowDialog(DialogState.NotificationPermission))
                    }
                }
            )

            ReviewSettingsCard(
                successesToAdvance = state.successesToAdvance,
                forgotPenalty = state.forgotPenalty,
                onShowReviewSettingsDialog = { onEvent(SettingsEvent.ShowDialog(DialogState.ReviewSettings)) }
            )

            WordManagerCard(onClick = onNavigateToWordManager)

            SubscriptionCard(onClick = onNavigateToSubscription)

            AboutSettingsCard(appVersion = state.appVersion)
        }

    }

    if (dialogState is DialogState.LanguageSelection) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onDismiss = { onEvent(SettingsEvent.DismissDialog) },
            onLanguageSelected = { language ->
                onEvent(SettingsEvent.SetLanguage(language))
                onEvent(SettingsEvent.DismissDialog)
            }
        )
    }

    if (dialogState is DialogState.ThemeSelection) {
        ThemeModeDialog(
            currentThemeMode = themeMode,
            onDismiss = { onEvent(SettingsEvent.DismissDialog) },
            onThemeModeSelected = { mode ->
                onEvent(SettingsEvent.SetThemeMode(mode))
                onEvent(SettingsEvent.DismissDialog)
            }
        )
    }

    if (dialogState is DialogState.NotificationPermission) {
        val deniedPreviously = wasNotificationPermissionDenied()
        val requestPermission = rememberNotificationPermissionRequester { granted ->
            if (granted) {
                onEvent(SettingsEvent.SetNotificationsEnabled(true))
                onEvent(SettingsEvent.DismissDialog)
                onEvent(SettingsEvent.RefreshNotificationPermissionStatus)
            } else {
                onEvent(SettingsEvent.DismissDialog)
                onEvent(SettingsEvent.RefreshNotificationPermissionStatus)
            }
        }
        NotificationPermissionDialog(
            onDismiss = { onEvent(SettingsEvent.DismissDialog) },
            onEnableNotifications = {
                if (deniedPreviously) {
                    onEvent(SettingsEvent.DismissDialog)
                    onEvent(SettingsEvent.RequestNotificationPermission)
                    onEvent(SettingsEvent.RefreshNotificationPermissionStatus)
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
            onNotificationsToggle = { onEvent(SettingsEvent.SetNotificationsEnabled(it)) },
            onDismiss = { onEvent(SettingsEvent.DismissDialog) }
        )
    }

    if (dialogState is DialogState.ReviewSettings) {
        ReviewSettingsDialog(
            successesToAdvance = state.successesToAdvance,
            forgotPenalty = state.forgotPenalty,
            onDismiss = { onEvent(SettingsEvent.DismissDialog) },
            onSettingsChanged = { successes, penalty ->
                onEvent(SettingsEvent.SetReviewSettings(successes, penalty))
                onEvent(SettingsEvent.DismissDialog)
            }
        )
    }
}

