package feature.auth.di

import feature.auth.AuthViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun authModule() = module {
    viewModel {
        AuthViewModel(
            loginWithGoogleUseCase = get(),
            loginWithAppleUseCase = get(),
            observeAuthStateUseCase = get(),
            verifySessionUseCase = get(),
            handleLoginSuccessUseCase = get(),
            deactivatePushTokenUseCase = get(),
            analyticsTracker = get(),
            userManager = get(),
        )
    }
}
