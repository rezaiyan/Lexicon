package di

import data.profile.remote.ProfileStatsRemoteDataSource
import data.profile.repository.ProfileStatsRepositoryImpl
import domain.profile.repository.IProfileStatsRepository
import domain.profile.usecase.GetProfileStatsUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun profileModule() = module {

    // Remote Data Sources
    single { ProfileStatsRemoteDataSource(apiClient = get()) }

    // Repositories
    single<IProfileStatsRepository> {
        ProfileStatsRepositoryImpl(remoteDataSource = get())
    }

    // Use Cases
    singleOf(::GetProfileStatsUseCase)
}
