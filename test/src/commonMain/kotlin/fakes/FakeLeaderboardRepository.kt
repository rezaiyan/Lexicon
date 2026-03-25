package fakes

import core.common.Try
import domain.leaderboard.model.Leaderboard
import domain.leaderboard.repository.ILeaderboardRepository

class FakeLeaderboardRepository : ILeaderboardRepository {
    var result: Try<Leaderboard> = Try.success(Leaderboard(emptyList(), null))

    override suspend fun getLeaderboard(): Try<Leaderboard> = result
}
