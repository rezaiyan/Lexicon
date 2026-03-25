package feature.auth.di

import feature.auth.AuthViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun authModule() = module {
    viewModel {
        AuthViewModel(
            loginWithGoogleUseCase = get(),
            loginWithAppleUseCase = get(),
            logoutUseCase = get(),
            observeAuthStateUseCase = get(),
            verifySessionUseCase = get(),
            syncRemoteToLocalUseCase = get(),
            initializePushNotificationsUseCase = get(),
            registerPushTokenUseCase = get(),
            analyticsTracker = get(),
            userManager = get(),
            subscriptionManager = get(),
        )
    }
}
