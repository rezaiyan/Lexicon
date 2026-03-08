package presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import feature.settings.SettingsViewModel
import components.scaffold.LexiconColumn
import overlay.LocalOverlayHost
import overlay.bottomsheet.showSizeToFitBottomSheet
import presentation.ui.components.LanguageSelectionContent
import presentation.ui.components.NotificationPermissionContent
import presentation.ui.components.NotificationSettingsContent
import presentation.ui.components.ThemeModeContent
import presentation.ui.components.settings.AboutSettingsCard
import presentation.ui.components.settings.LanguageSettingsCard
import presentation.ui.components.settings.NotificationSettingsCard
import presentation.ui.components.settings.SubscriptionCard
import presentation.ui.components.settings.ThemeSettingsCard
import presentation.ui.components.settings.WordManagerCard
import presentation.ui.permissions.rememberNotificationPermissionRequester
import presentation.ui.permissions.wasNotificationPermissionDenied
import presentation.ui.screens.settings.showWordManagerSheet
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.settings

@Composable
fun SettingsScreen(
    onNavigateToSubscription: () -> Unit = {},
) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val settingsState by viewModel.state()
    val state = settingsState.screen
    val currentLanguage = state.currentLanguage
    val themeMode = state.themeMode
    val notificationsEnabled = state.notificationsEnabled
    val systemNotificationsEnabled = state.systemNotificationsEnabled
    val overlayHost = LocalOverlayHost.current

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
                    onShowLanguageDialog = {
                        overlayHost.showSizeToFitBottomSheet(tag = "language-selection") { nav ->
                            LanguageSelectionContent(
                                currentLanguage = currentLanguage,
                                onLanguageSelected = { language ->
                                    viewModel.setLanguage(language)
                                    nav.dismiss()
                                }
                            )
                        }
                    }
                )
            }

            ThemeSettingsCard(
                themeMode = themeMode,
                onShowThemeDialog = {
                    overlayHost.showSizeToFitBottomSheet(tag = "theme-selection") { nav ->
                        ThemeModeContent(
                            currentThemeMode = themeMode,
                            onThemeModeSelected = { mode ->
                                viewModel.setThemeMode(mode)
                                nav.dismiss()
                            }
                        )
                    }
                }
            )

            NotificationSettingsCard(
                systemNotificationsEnabled = systemNotificationsEnabled,
                notificationsEnabled = notificationsEnabled,
                onEnable = {
                    if (systemNotificationsEnabled) {
                        overlayHost.showSizeToFitBottomSheet(tag = "notification-settings") { nav ->
                            val currentState by viewModel.state()
                            NotificationSettingsContent(
                                notificationsEnabled = currentState.screen.notificationsEnabled,
                                systemNotificationsEnabled = currentState.screen.systemNotificationsEnabled,
                                onNotificationsToggle = { viewModel.setNotificationsEnabled(it) },
                                onDismiss = { nav.dismiss() }
                            )
                        }
                    } else {
                        overlayHost.showSizeToFitBottomSheet(tag = "notification-permission") { nav ->
                            val deniedPreviously = wasNotificationPermissionDenied()
                            val requestPermission = rememberNotificationPermissionRequester { granted ->
                                if (granted) {
                                    viewModel.setNotificationsEnabled(true)
                                }
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
                                }
                            )
                        }
                    }
                }
            )

            WordManagerCard(onClick = { overlayHost.showWordManagerSheet() })

            SubscriptionCard(onClick = onNavigateToSubscription)

            AboutSettingsCard(appVersion = state.appVersion)
        }

    }
}
