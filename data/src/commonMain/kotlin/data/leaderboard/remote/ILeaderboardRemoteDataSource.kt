package data.leaderboard.remote

import data.leaderboard.remote.model.LeaderboardResponse
import core.common.Try

interface ILeaderboardRemoteDataSource {
    suspend fun getLeaderboard(): Try<LeaderboardResponse>
}
