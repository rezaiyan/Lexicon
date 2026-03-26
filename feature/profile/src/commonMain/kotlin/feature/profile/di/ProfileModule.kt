package feature.profile.di

import domain.profile.usecase.EnrichProfileStatsUseCase
import domain.profile.usecase.ValidateDisplayAliasUseCase
import feature.profile.EditProfileViewModel
import feature.profile.ProfileViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun profileModule() = module {
    singleOf(::ValidateDisplayAliasUseCase)
    singleOf(::EnrichProfileStatsUseCase)

    viewModel {
        ProfileViewModel(
            userManager = get(),
            getFeatureAccessUseCase = get(),
            streakManager = get(),
            getProfileStatsUseCase = get(),
            enrichProfileStatsUseCase = get(),
        )
    }
    viewModel {
        EditProfileViewModel(
            userManager = get(),
            updateProfileUseCase = get(),
            uploadAvatarUseCase = get(),
            deleteAvatarUseCase = get(),
            validateDisplayAliasUseCase = get(),
        )
    }
}
