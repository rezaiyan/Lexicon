package data.streak.remote

import data.core.network.client.ApiClient
import data.streak.remote.model.RecordActivityRequest
import data.streak.remote.model.StreakResponse
import core.common.Try
import core.common.doOnFailure
import core.common.doOnSuccess
import expects.logNetwork

/**
 * Remote data source for streak tracking operations
 * Handles fetching and recording user learning streaks
 */
class StreakRemoteDataSource(
    private val apiClient: ApiClient
) : IStreakRemoteDataSource {

    override suspend fun getStreak(): Try<StreakResponse> =
        apiClient.getNotNull<StreakResponse>("/streak")
            .doOnSuccess { response ->
                logNetwork("StreakRemoteDataSource", "Streak retrieved: ${response.currentStreak}")
            }

    override suspend fun recordActivity(count: Int): Try<StreakResponse> =
        apiClient.postNotNull<StreakResponse>("/streak/record", body = RecordActivityRequest(count))
            .doOnSuccess { response ->
                logNetwork("StreakRemoteDataSource", "Activity recorded: streak=${response.currentStreak}")
            }
            .doOnFailure { error ->
                logNetwork("StreakRemoteDataSource", "Error recording activity: ${error.message}")
            }
}
