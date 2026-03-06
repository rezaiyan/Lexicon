package feature.profile.di

import feature.profile.EditProfileViewModel
import feature.profile.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun profileModule() = module {
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
}
