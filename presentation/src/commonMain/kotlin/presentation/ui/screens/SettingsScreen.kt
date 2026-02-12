package presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import presentation.feature.settings.SettingsViewModel
import presentation.model.DialogState
import presentation.model.SettingsScreenState
import presentation.ui.components.BasicAlertDialog
import presentation.ui.components.ButtonState
import presentation.ui.components.LanguageSelectionDialog
import presentation.ui.components.NotificationPermissionDialog
import presentation.ui.components.NotificationSettingsDialog
import presentation.ui.components.ThemeModeDialog
import presentation.ui.components.LexiconColumn
import presentation.ui.components.settings.AboutSettingsCard
import presentation.ui.components.settings.CollectionsCard
import presentation.ui.components.settings.LanguageSettingsCard
import presentation.ui.components.settings.NotificationSettingsCard
import presentation.ui.components.settings.ReviewSettingsCard
import presentation.ui.components.settings.SubscriptionCard
import presentation.ui.components.settings.ThemeSettingsCard
import presentation.ui.components.settings.WordManagerCard
import presentation.ui.permissions.rememberNotificationPermissionRequester
import presentation.ui.permissions.wasNotificationPermissionDenied
import theme.Theme
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.apply_button
import vokab.resources.generated.resources.cancel
import vokab.resources.generated.resources.choose_learning_pace
import vokab.resources.generated.resources.learning_mode
import vokab.resources.generated.resources.mode_balanced
import vokab.resources.generated.resources.mode_balanced_description
import vokab.resources.generated.resources.mode_balanced_subtitle
import vokab.resources.generated.resources.mode_easy
import vokab.resources.generated.resources.mode_easy_description
import vokab.resources.generated.resources.mode_easy_subtitle
import vokab.resources.generated.resources.mode_rigorous
import vokab.resources.generated.resources.mode_rigorous_description
import vokab.resources.generated.resources.mode_rigorous_subtitle
import vokab.resources.generated.resources.settings

@Composable
fun SettingsScreen(
    onNavigateToWordManager: () -> Unit,
    onNavigateToCollection: () -> Unit,
    onNavigateToSubscription: () -> Unit = {},
) {
    val settingsViewModel = koinInject<SettingsViewModel>()
    val state by settingsViewModel.settingsScreenState.collectAsStateWithLifecycle()
    val dialogState by settingsViewModel.dialogState.collectAsStateWithLifecycle()

    SettingsScreenContent(
        state = state,
        dialogState = dialogState,
        viewModel = settingsViewModel,
        onNavigateToWordManager = onNavigateToWordManager,
        onNavigateToCollection = onNavigateToCollection,
        onNavigateToSubscription = onNavigateToSubscription,
    )
}

@Composable
private fun SettingsScreenContent(
    state: SettingsScreenState,
    dialogState: DialogState,
    viewModel: SettingsViewModel,
    onNavigateToWordManager: () -> Unit,
    onNavigateToCollection: () -> Unit,
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
                    onShowLanguageDialog = { viewModel.showLanguageDialog() }
                )
            }

            ThemeSettingsCard(
                themeMode = themeMode,
                onShowThemeDialog = { viewModel.showThemeDialog() }
            )

            NotificationSettingsCard(
                systemNotificationsEnabled = systemNotificationsEnabled,
                notificationsEnabled = notificationsEnabled,
                onEnable = {
                    if (systemNotificationsEnabled) {
                        viewModel.showNotificationSettingsDialog()
                    } else {
                        viewModel.showNotificationPermissionDialog()
                    }
                }
            )

            ReviewSettingsCard(
                successesToAdvance = state.successesToAdvance,
                forgotPenalty = state.forgotPenalty,
                onShowReviewSettingsDialog = { viewModel.showReviewSettingsDialog() }
            )

            WordManagerCard(onClick = onNavigateToWordManager)
            if (state.isPremiumFeatureEnabled) {
                CollectionsCard(onClick = onNavigateToCollection)
            }

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
                    // User already denied once; now explicitly send them to settings
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

    if (dialogState is DialogState.ReviewSettings) {
        ReviewSettingsDialog(
            successesToAdvance = state.successesToAdvance,
            forgotPenalty = state.forgotPenalty,
            onDismiss = { viewModel.dismissDialog() },
            onSettingsChanged = { successes, penalty ->
                viewModel.setReviewSettings(successes, penalty)
                viewModel.dismissDialog()
            }
        )
    }
}


@Composable
fun ReviewSettingsDialog(
    successesToAdvance: Int,
    forgotPenalty: Int,
    onDismiss: () -> Unit,
    onSettingsChanged: (successesToAdvance: Int, forgotPenalty: Int) -> Unit
) {
    var selectedMode by remember {
        mutableStateOf(
            when {
                successesToAdvance == 1 && forgotPenalty == 1 -> 0 // Easy
                successesToAdvance == 1 && forgotPenalty == 2 -> 1 // Balanced
                successesToAdvance == 2 && forgotPenalty == 3 -> 2 // Rigorous
                else -> 1 // Default to Balanced
            }
        )
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.learning_mode),
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall2)
            ) {
                Text(
                    text = stringResource(Res.string.choose_learning_pace),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Theme.spacing.extraSmall2)
                )

                LearningModeOption(
                    title = stringResource(Res.string.mode_easy),
                    subtitle = stringResource(Res.string.mode_easy_subtitle),
                    description = stringResource(Res.string.mode_easy_description),
                    selected = selectedMode == 0,
                    onClick = { selectedMode = 0 }
                )

                LearningModeOption(
                    title = stringResource(Res.string.mode_balanced),
                    subtitle = stringResource(Res.string.mode_balanced_subtitle),
                    description = stringResource(Res.string.mode_balanced_description),
                    selected = selectedMode == 1,
                    onClick = { selectedMode = 1 }
                )

                LearningModeOption(
                    title = stringResource(Res.string.mode_rigorous),
                    subtitle = stringResource(Res.string.mode_rigorous_subtitle),
                    description = stringResource(Res.string.mode_rigorous_description),
                    selected = selectedMode == 2,
                    onClick = { selectedMode = 2 }
                )
            }
        },
        primaryButton = ButtonState(
            text = stringResource(Res.string.apply_button),
            onClick = {
                val (successes, penalty) = when (selectedMode) {
                    0 -> 1 to 1
                    1 -> 1 to 2
                    2 -> 2 to 3
                    else -> 1 to 2
                }
                onSettingsChanged(successes, penalty)
            }
        ),
        secondaryButton = ButtonState(
            text = stringResource(Res.string.cancel),
            onClick = onDismiss
        )
    )
}

@Composable
private fun LearningModeOption(
    title: String,
    subtitle: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(Theme.spacing.small))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = Theme.spacing.extraSmall4)
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    },
                    modifier = Modifier.padding(top = Theme.spacing.extraSmall3)
                )
            }
        }
    }
}

