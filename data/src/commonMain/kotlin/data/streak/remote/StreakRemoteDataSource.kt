package data.streak.remote

import data.core.network.client.ApiClient
import data.streak.remote.model.RecordActivityRequest
import data.streak.remote.model.StreakResponse
import domain.common.Try
import domain.common.doOnFailure
import domain.common.doOnSuccess
import expects.logNetwork

/**
 * Remote data source for streak tracking operations
 * Handles fetching and recording user learning streaks
 */
class StreakRemoteDataSource(
    private val apiClient: ApiClient
) {

    suspend fun getStreak(): Try<StreakResponse> =
        apiClient.getNotNull<StreakResponse>("/streak")
            .doOnSuccess { response ->
                logNetwork("StreakRemoteDataSource", "Streak retrieved: ${response.currentStreak}")
            }

    suspend fun recordActivity(count: Int): Try<StreakResponse> =
        apiClient.postNotNull<StreakResponse>("/streak/record", body = RecordActivityRequest(count))
            .doOnSuccess { response ->
                logNetwork("StreakRemoteDataSource", "Activity recorded: streak=${response.currentStreak}")
            }
            .doOnFailure { error ->
                logNetwork("StreakRemoteDataSource", "Error recording activity: ${error.message}")
            }
}
