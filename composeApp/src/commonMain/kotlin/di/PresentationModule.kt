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
import presentation.ui.components.imports.ImportViewModel
import presentation.viewmodel.AppNavigationViewModel
import feature.auth.di.authModule
import feature.study.di.studyModule
import feature.words.di.wordsModule
import feature.profile.di.profileModule
import feature.settings.di.settingsModule
import feature.onboarding.di.onboardingModule
import feature.subscription.di.subscriptionModule
import feature.leaderboard.di.leaderboardModule
import feature.aiimport.di.importModule

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
            registerPushTokenUseCase = get(),
        )
    }

    // Streak Manager
    single<IStreakManager> {
        StreakManagerImpl(streakRepository = get())
    }

    // App Navigation (stays in presentation — app-level coordinator)
    viewModel { AppNavigationViewModel(onboardingRepository = get()) }

    // Import VM (stays in presentation — depends on Compose UI types)
    viewModel {
        ImportViewModel(
            getFeatureAccessUseCase = get(),
            importWordsUseCase = get(),
            importViaFileUseCase = get(),
            importFromImageUseCase = get(),
            userManager = get(),
            getCurrentLanguageUseCase = get(),
            getSourceLanguageUseCase = get(),
            performanceTracer = get(),
        )
    }

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
        importModule(),
    )
}
