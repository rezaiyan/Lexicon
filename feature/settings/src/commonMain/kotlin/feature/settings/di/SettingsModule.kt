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
            setLanguageUseCase = get(),
            setThemeModeUseCase = get(),
            setNotificationsEnabledUseCase = get(),
            requestNotificationPermissionUseCase = get(),
            openNotificationSettingsUseCase = get(),
            analyticsTracker = get(),
            authRepository = get(),
            notificationPermissionMonitor = get(),
            appVersionProvider = get(),
        )
    }
}
