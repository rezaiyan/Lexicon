package di

import data.profile.remote.ProfileRemoteDataSource
import data.profile.remote.ProfileStatsRemoteDataSource
import data.profile.repository.ProfileRepositoryImpl
import data.profile.repository.ProfileStatsRepositoryImpl
import domain.profile.repository.IProfileRepository
import domain.profile.repository.IProfileStatsRepository
import domain.profile.usecase.DeleteAvatarUseCase
import domain.profile.usecase.GetProfileStatsUseCase
import domain.profile.usecase.UpdateProfileUseCase
import domain.profile.usecase.UploadAvatarUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun profileModule() = module {

    // Remote Data Sources
    single { ProfileStatsRemoteDataSource(apiClient = get()) }
    single { ProfileRemoteDataSource(apiClient = get()) }

    // Repositories
    single<IProfileStatsRepository> {
        ProfileStatsRepositoryImpl(remoteDataSource = get())
    }
    single<IProfileRepository> {
        ProfileRepositoryImpl(remoteDataSource = get())
    }

    // Use Cases
    singleOf(::GetProfileStatsUseCase)
    singleOf(::UpdateProfileUseCase)
    singleOf(::UploadAvatarUseCase)
    singleOf(::DeleteAvatarUseCase)
}
