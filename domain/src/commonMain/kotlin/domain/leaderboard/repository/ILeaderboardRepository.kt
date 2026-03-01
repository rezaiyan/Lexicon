package domain.leaderboard.repository

import domain.common.Try
import domain.leaderboard.model.Leaderboard

interface ILeaderboardRepository {
    suspend fun getLeaderboard(): Try<Leaderboard>
}
