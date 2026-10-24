package feature.settings.di

import feature.settings.ConnectedAppsViewModel
import feature.settings.NotificationPermissionMonitor
import feature.settings.PasskeysViewModel
import feature.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun settingsModule() = module {
    single { NotificationPermissionMonitor(notificationRepository = get()) }
    viewModelOf(::ConnectedAppsViewModel)
    viewModelOf(::PasskeysViewModel)

    viewModel {
        SettingsViewModel(
            settingsRepository = get(),
            notificationRepository = get(),
            setThemeModeUseCase = get(),
            setNotificationsEnabledUseCase = get(),
            setReviewRemindersEnabledUseCase = get(),
            requestNotificationPermissionUseCase = get(),
            openNotificationSettingsUseCase = get(),
            analyticsTracker = get(),
            notificationPermissionMonitor = get(),
            appVersionProvider = get(),
            getTtsModelsInfoUseCase = get(),
            deleteTtsModelUseCase = get(),
            downloadTtsModelUseCase = get(),
            setTtsSpeechRateUseCase = get(),
            setTtsVoiceUseCase = get(),
            setTtsExpressivenessUseCase = get(),
            selectTtsVoiceUseCase = get(),
            speakWordUseCase = get(),
            stopSpeakingUseCase = get(),
            getDailyGoalWordsUseCase = get(),
            setDailyGoalWordsUseCase = get(),
        )
    }
}
