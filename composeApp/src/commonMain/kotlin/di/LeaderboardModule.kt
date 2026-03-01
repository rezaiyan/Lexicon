package di

import data.leaderboard.remote.LeaderboardRemoteDataSource
import data.leaderboard.repository.LeaderboardRepositoryImpl
import domain.leaderboard.repository.ILeaderboardRepository
import domain.leaderboard.usecase.GetLeaderboardUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun leaderboardModule() = module {
    single { LeaderboardRemoteDataSource(apiClient = get()) }

    single<ILeaderboardRepository> {
        LeaderboardRepositoryImpl(remoteDataSource = get())
    }

    singleOf(::GetLeaderboardUseCase)
}
