package feature.settings.di

import feature.settings.NotificationPermissionMonitor
import feature.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun settingsModule() = module {
    single { NotificationPermissionMonitor(notificationRepository = get()) }

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
