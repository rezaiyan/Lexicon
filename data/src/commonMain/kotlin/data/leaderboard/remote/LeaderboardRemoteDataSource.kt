package data.leaderboard.remote

import data.core.network.client.ApiClient
import data.leaderboard.remote.model.LeaderboardResponse
import core.common.Try
import core.common.doOnSuccess
import expects.logNetwork

class LeaderboardRemoteDataSource(
    private val apiClient: ApiClient
) {
    suspend fun getLeaderboard(): Try<LeaderboardResponse> =
        apiClient.getNotNull<LeaderboardResponse>("/leaderboard")
            .doOnSuccess { response ->
                logNetwork(
                    "LeaderboardRemoteDataSource",
                    "Leaderboard retrieved: ${response.entries.size} entries"
                )
            }
}
