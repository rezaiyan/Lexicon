package presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import components.GroupedSection
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import feature.settings.SettingsViewModel
import components.scaffold.LexiconColumn
import overlay.LocalOverlayHost
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import overlay.bottomsheet.showSizeToFitBottomSheet
import presentation.ui.components.LanguageSelectionContent
import presentation.ui.components.ThemeModeContent
import presentation.ui.components.DailyGoalContent
import presentation.ui.components.settings.AboutSettingsCard
import presentation.ui.components.settings.DailyGoalSettingsCard
import presentation.ui.components.settings.LanguageSettingsCard
import presentation.ui.components.settings.NotificationSettingsCard
import presentation.ui.components.settings.ThemeSettingsCard
import presentation.ui.components.settings.TtsModelCacheCard
import presentation.ui.components.settings.TtsDeleteConfirmationContent
import presentation.ui.components.settings.TtsVoiceManagerContent
import presentation.ui.components.settings.TagManagerCard
import presentation.ui.components.settings.showNotificationSettingsSheet
import presentation.ui.screens.settings.showTagManagerScreen
import theme.Theme
import lexicon.resources.generated.resources.Res
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
    val currentLanguage = state.currentLanguage
    val themeMode = state.themeMode
    val notificationsEnabled = state.notificationsEnabled
    val systemNotificationsEnabled = state.systemNotificationsEnabled
    val overlayHost = LocalOverlayHost.current

    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg)) {
        GroupedSection(title = stringResource(Res.string.learning)) {
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
                                },
                                onClose = { nav.dismiss() },
                            )
                        }
                    }
                )
            }

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
                        BottomSheetPages(navigator = pages, onClose = { nav.dismiss() }, label = "ttsPages") { page ->
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

            AboutSettingsCard(appVersion = state.appVersion, showDivider = false)
        }
    }
}

private sealed interface TtsSheetPage {
    data object Voices : TtsSheetPage
    data class ConfirmDelete(val languageCode: String, val displayName: String) : TtsSheetPage
}
