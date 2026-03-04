package domain.leaderboard.repository

import core.common.Try
import domain.leaderboard.model.Leaderboard

interface ILeaderboardRepository {
    suspend fun getLeaderboard(): Try<Leaderboard>
}
