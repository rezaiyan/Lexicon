package di

import analytics.IAnalyticsTracker
import analytics.createAnalyticsTracker
import domain.auth.manager.IUserManager
import domain.featureflag.IFeatureFlagProvider
import domain.streak.manager.IStreakManager
import featureflag.createFeatureFlagProvider
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import performance.IPerformanceTracer
import performance.createPerformanceTracer
import presentation.manager.StreakManagerImpl
import presentation.manager.UserManagerImpl
import domain.word.usecase.ClassifyImportErrorUseCase
import domain.word.usecase.ObserveImageImportAccessUseCase
import org.koin.core.module.dsl.singleOf
import presentation.viewmodel.AppNavigationViewModel
import feature.auth.di.authModule
import feature.study.di.studyModule
import feature.words.di.wordsModule
import feature.profile.di.profileModule
import feature.settings.di.settingsModule
import feature.onboarding.di.onboardingModule
import feature.subscription.di.subscriptionModule
import feature.leaderboard.di.leaderboardModule
import feature.addwords.di.addWordsPresentationModule
import domain.analytics.usecase.RetryAnalyticsSyncUseCase
import domain.startup.usecase.DetermineAppStartupStateUseCase
import domain.startup.usecase.DeterminePostAuthDestinationUseCase
import feature.insights.di.insightsModule

fun presentationModule() = module {

    // Analytics Tracker (platform-specific)
    single<IAnalyticsTracker> { createAnalyticsTracker() }

    // KAN-15: Performance Tracer (platform-specific)
    single<IPerformanceTracer> { createPerformanceTracer() }

    // KAN-20: Feature Flag Provider (platform-specific)
    single<IFeatureFlagProvider> { createFeatureFlagProvider() }

    // User Manager
    single<IUserManager> {
        UserManagerImpl(
            logoutUseCase = get(),
            deleteAccountUseCase = get(),
            subscriptionManager = get(),
            streakManager = get(),
            deactivatePushTokenUseCase = get(),
        )
    }

    // Streak Manager
    single<IStreakManager> {
        StreakManagerImpl(streakRepository = get())
    }

    // Startup use cases
    singleOf(::DetermineAppStartupStateUseCase)
    singleOf(::DeterminePostAuthDestinationUseCase)

    // App Navigation (stays in presentation — app-level coordinator)
    viewModel {
        AppNavigationViewModel(
            onboardingRepository = get(),
            retryAnalyticsSyncUseCase = get<RetryAnalyticsSyncUseCase>(),
            determineAppStartupStateUseCase = get(),
            determinePostAuthDestinationUseCase = get(),
            addStarterWordsUseCase = get(),
            uploadPendingWordsUseCase = get(),
            analytics = get(),
        )
    }

    singleOf(::ClassifyImportErrorUseCase)
    singleOf(::ObserveImageImportAccessUseCase)

    // Feature modules
    includes(
        authModule(),
        studyModule(),
        wordsModule(),
        profileModule(),
        settingsModule(),
        onboardingModule(),
        subscriptionModule(),
        leaderboardModule(),
        addWordsPresentationModule(),
        insightsModule(),
    )
}
