package di

import analytics.IAnalyticsTracker
import analytics.createAnalyticsTracker
import domain.auth.manager.IUserManager
import domain.streak.manager.IStreakManager
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import feature.auth.AuthViewModel
import presentation.feature.profile.EditProfileViewModel
import presentation.feature.profile.ProfileViewModel
import presentation.feature.settings.NotificationPermissionMonitor
import presentation.feature.settings.SettingsViewModel
import presentation.feature.study.StudyViewModel
import presentation.feature.subscription.SubscriptionViewModel
import presentation.manager.StreakManagerImpl
import presentation.manager.UserManagerImpl
import presentation.ui.components.imports.ImportViewModel
import presentation.feature.aiimport.AiWordImportViewModel
import presentation.feature.leaderboard.LeaderboardViewModel
import presentation.feature.onboarding.OnboardingViewModel
import presentation.feature.onboarding.VocabularyPreviewViewModel
import presentation.viewmodel.AppNavigationViewModel
import presentation.viewmodel.VocabularyViewModel
import presentation.viewmodel.WordManagerViewModel

fun presentationModule() = module {

    // Analytics Tracker (platform-specific)
    single<IAnalyticsTracker> { createAnalyticsTracker() }

    // User Manager
    single<IUserManager> {
        UserManagerImpl(
            logoutUseCase = get(),
            deleteAccountUseCase = get(),
            subscriptionManager = get(),
            streakManager = get(),
            registerPushTokenUseCase = get(),
        )
    }

    // Streak Manager
    single<IStreakManager> {
        StreakManagerImpl(streakRepository = get())
    }

    // Notification Permission Monitor
    single { NotificationPermissionMonitor(notificationRepository = get()) }

    // ViewModels (properly scoped)
    viewModel {
        AuthViewModel(
            loginWithGoogleUseCase = get(),
            loginWithAppleUseCase = get(),
            logoutUseCase = get(),
            isAuthenticatedUseCase = get(),
            verifySessionUseCase = get(),
            syncRemoteToLocalUseCase = get(),
            initializePushNotificationsUseCase = get(),
            registerPushTokenUseCase = get(),
            analyticsTracker = get(),
            userManager = get(),
            subscriptionManager = get(),
        )
    }

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

    viewModel { AppNavigationViewModel(onboardingRepository = get()) }

    // Screen-Scoped ViewModels
    viewModel {
        StudyViewModel(
            getProgressStatsUseCase = get(),
            evaluateProgressUseCase = get(),
            getFeatureAccessUseCase = get(),
            scheduleNotificationsUseCase = get(),
            getDueWordsUseCase = get(),
            getWordsByStageUseCase = get(),
            reviewWordUseCase = get(),
            updateWordUseCase = get(),
            deleteWordUseCase = get(),
            recordStreakActivityUseCase = get(),
            speakWordUseCase = get(),
            ttsRepository = get(),
            analyticsTracker = get(),
        )
    }

    viewModel {
        ImportViewModel(
            getFeatureAccessUseCase = get(),
            importWordsUseCase = get(),
            importViaFileUseCase = get(),
            importFromImageUseCase = get(),
            userManager = get(),
            getCurrentLanguageUseCase = get(),
        )
    }

    viewModel {
        VocabularyViewModel(
            getDueWordsUseCase = get(),
            getWordsByStageUseCase = get(),
            updateWordUseCase = get(),
            deleteWordUseCase = get(),
            analyticsTracker = get(),
        )
    }
    viewModel {
        WordManagerViewModel(
            getAllWordsUseCase = get(),
            deleteWordsUseCase = get(),
            batchUpdateLanguagesUseCase = get(),
            updateWordUseCase = get(),
            exportWordsUseCase = get(),
            getFeatureAccessUseCase = get(),
            analyticsTracker = get(),
        )
    }


    viewModel {
        ProfileViewModel(
            userManager = get(),
            getFeatureAccessUseCase = get(),
            streakManager = get(),
            getProfileStatsUseCase = get()
        )
    }

    viewModel {
        EditProfileViewModel(
            userManager = get(),
            updateProfileUseCase = get(),
            uploadAvatarUseCase = get(),
            deleteAvatarUseCase = get()
        )
    }

    viewModel {
        SubscriptionViewModel(
            subscriptionManager = get()
        )
    }

    viewModel {
        LeaderboardViewModel(
            getLeaderboardUseCase = get()
        )
    }

    // Onboarding ViewModels
    viewModel {
        OnboardingViewModel(
            submitPreferencesUseCase = get(),
            setLanguageUseCase = get()
        )
    }

    viewModel { VocabularyPreviewViewModel() }

    // AI Word Import
    viewModel {
        AiWordImportViewModel(
            submitPreferencesUseCase = get(),
            importSuggestedVocabularyUseCase = get()
        )
    }
}
