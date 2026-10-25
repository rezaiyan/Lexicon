package presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import components.GroupedSection
import feature.profile.navigation.showDeleteAccountSheet
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import feature.settings.ConnectedAppsViewModel
import feature.settings.PasskeysViewModel
import feature.settings.ServerStatusViewModel
import feature.settings.SettingsViewModel
import components.scaffold.LexiconColumn
import overlay.LocalOverlayHost
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import overlay.bottomsheet.showSizeToFitBottomSheet
import presentation.ui.components.ThemeModeContent
import presentation.ui.components.DailyGoalContent
import feature.support.SupportBadgeViewModel
import feature.support.navigation.showSupport
import presentation.ui.components.settings.AboutSettingsCard
import presentation.ui.components.settings.ConnectedAppsSettingsCard
import presentation.ui.components.settings.showConnectedAppsSheet
import presentation.ui.components.settings.PasskeysSettingsCard
import presentation.ui.components.settings.ServerStatusNotice
import presentation.ui.components.settings.showPasskeysSheet
import presentation.ui.components.settings.DailyGoalSettingsCard
import presentation.ui.components.settings.NotificationSettingsCard
import presentation.ui.components.settings.SupportSettingsCard
import presentation.ui.components.settings.ThemeSettingsCard
import presentation.ui.components.settings.TtsModelCacheCard
import presentation.ui.components.settings.TtsDeleteConfirmationContent
import presentation.ui.components.settings.TtsVoiceManagerContent
import presentation.ui.components.settings.TagManagerCard
import presentation.ui.components.settings.showNotificationSettingsSheet
import presentation.ui.screens.settings.showTagManagerScreen
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.delete_account
import lexicon.resources.generated.resources.learning
import lexicon.resources.generated.resources.navigate_back
import lexicon.resources.generated.resources.settings

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
) {
    LexiconColumn(
        title = stringResource(Res.string.settings),
        showNavigationIcon = true,
        navigationIconContentDescription = stringResource(Res.string.navigate_back),
        onNavigationClick = onNavigateBack,
        scrollable = true,
    ) {
        Column(modifier = Modifier.padding(top = Theme.spacing.xs, bottom = Theme.spacing.xl)) {
            SettingsSections()
        }
    }
}

/** Learning + app settings as grouped sections. Profile links here instead of repeating them. */
@Composable
private fun SettingsSections() {
    val viewModel = koinViewModel<SettingsViewModel>()
    val settingsState by viewModel.state()
    val state = settingsState.screen
    val themeMode = state.themeMode
    val notificationsEnabled = state.notificationsEnabled
    val systemNotificationsEnabled = state.systemNotificationsEnabled
    val overlayHost = LocalOverlayHost.current
    val supportBadge = koinViewModel<SupportBadgeViewModel>()
    val supportUnread by supportBadge.state()
    LaunchedEffect(supportBadge) { supportBadge.onSettingsOpened() }
    val connectedAppsViewModel = koinViewModel<ConnectedAppsViewModel>()
    val connectedApps by connectedAppsViewModel.state()
    val passkeysViewModel = koinViewModel<PasskeysViewModel>()
    val passkeys by passkeysViewModel.state()
    val serverStatusViewModel = koinViewModel<ServerStatusViewModel>()
    val serverStatus by serverStatusViewModel.state()

    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg)) {
        // Nothing while connected; a slim notice on top when the server can't be reached
        ServerStatusNotice(
            status = serverStatus.status,
            retrying = serverStatus.retrying,
            onRetry = serverStatusViewModel::retry,
        )

        GroupedSection(title = stringResource(Res.string.learning)) {
            DailyGoalSettingsCard(
                onClick = {
                    overlayHost.showSizeToFitBottomSheet(tag = "daily-goal") { nav ->
                        val currentState by viewModel.state()
                        DailyGoalContent(
                            selectedGoal = currentState.dailyGoalWords,
                            onGoalSelected = { count ->
                                viewModel.setDailyGoalWords(count)
                                nav.dismiss()
                            },
                            onClose = { nav.dismiss() },
                        )
                    }
                }
            )

            NotificationSettingsCard(
                systemNotificationsEnabled = systemNotificationsEnabled,
                notificationsEnabled = notificationsEnabled,
                onEnable = { overlayHost.showNotificationSettingsSheet(viewModel) },
            )

            TagManagerCard(onClick = { overlayHost.showTagManagerScreen() })

            TtsModelCacheCard(
                showDivider = false,
                onClick = {
                    viewModel.loadTtsModels()
                    overlayHost.showSizeToFitBottomSheet(tag = "tts-model-cache") { nav ->
                        val currentState by viewModel.state()
                        val pages = rememberBottomSheetPageNavigator<TtsSheetPage>(TtsSheetPage.Voices)
                        BottomSheetPages(
                            navigator = pages,
                            onClose = {
                                viewModel.stopTtsPreview()
                                nav.dismiss()
                            },
                            label = "ttsPages",
                        ) { page ->
                            when (page) {
                                TtsSheetPage.Voices -> TtsVoiceManagerContent(
                                    models = currentState.ttsModels,
                                    isLoading = currentState.ttsModelsLoading,
                                    totalSizeBytes = currentState.ttsTotalSizeBytes,
                                    downloadProgress = currentState.ttsDownloadProgress,
                                    ttsSettings = currentState.ttsSettings,
                                    onSpeechRateChanged = { rate -> viewModel.setTtsSpeechRate(rate) },
                                    onDownloadModel = { languageCode -> viewModel.downloadTtsModel(languageCode) },
                                    onDeleteModel = { languageCode ->
                                        val model = currentState.ttsModels.find { it.languageCode == languageCode }
                                        pages.navigateTo(
                                            TtsSheetPage.ConfirmDelete(
                                                languageCode = languageCode,
                                                displayName = model?.languageDisplayName ?: languageCode,
                                            )
                                        )
                                    },
                                    onVoiceSelected = { languageCode, speakerId ->
                                        viewModel.setTtsVoice(languageCode, speakerId)
                                    },
                                    previewLanguage = currentState.ttsPreviewLanguage,
                                    onExpressivenessChanged = viewModel::setTtsExpressiveness,
                                    onVoiceModelSelected = viewModel::selectTtsModelVoice,
                                    onPreview = viewModel::previewTtsVoice,
                                    onStopPreview = viewModel::stopTtsPreview,
                                )

                                is TtsSheetPage.ConfirmDelete -> TtsDeleteConfirmationContent(
                                    languageDisplayName = page.displayName,
                                    onConfirm = {
                                        viewModel.deleteTtsModel(page.languageCode)
                                        pages.navigateBack()
                                    },
                                    onDismiss = { pages.navigateBack() },
                                )
                            }
                        }
                    }
                }
            )
        }

        GroupedSection(title = stringResource(Res.string.settings)) {
            ThemeSettingsCard(
                themeMode = themeMode,
                onShowThemeDialog = {
                    overlayHost.showSizeToFitBottomSheet(tag = "theme-selection") { nav ->
                        ThemeModeContent(
                            currentThemeMode = themeMode,
                            onThemeModeSelected = { mode ->
                                viewModel.setThemeMode(mode)
                                nav.dismiss()
                            },
                            onClose = { nav.dismiss() },
                        )
                    }
                }
            )

            SupportSettingsCard(
                hasUnreadReply = supportUnread.inChat,
                onClick = { overlayHost.showSupport() },
            )

            if (passkeys.available) {
                PasskeysSettingsCard(onClick = { overlayHost.showPasskeysSheet(passkeysViewModel) })
            }

            if (connectedApps.apps.isNotEmpty()) {
                ConnectedAppsSettingsCard(onClick = { overlayHost.showConnectedAppsSheet(connectedAppsViewModel) })
            }

            AboutSettingsCard(appVersion = state.appVersion, showDivider = false)
        }

        // Plain muted link under everything else so it's findable but never draws taps.
        TextButton(
            onClick = { overlayHost.showDeleteAccountSheet() },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = stringResource(Res.string.delete_account),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private sealed interface TtsSheetPage {
    data object Voices : TtsSheetPage
    data class ConfirmDelete(val languageCode: String, val displayName: String) : TtsSheetPage
}
