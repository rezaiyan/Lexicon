package feature.leaderboard.di

import feature.leaderboard.LeaderboardViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun leaderboardModule() = module {
    viewModel {
        LeaderboardViewModel(
            getLeaderboardUseCase = get()
        )
    }
}
