package data.streak.remote

import data.core.network.client.ApiClient
import data.streak.remote.model.StreakResponse
import expects.logNetwork

/**
 * Remote data source for streak tracking operations
 * Handles fetching and recording user learning streaks
 */
class StreakRemoteDataSource(
    private val apiClient: ApiClient
) {

    suspend fun getStreak(): Result<StreakResponse> =
        apiClient.getNotNull<StreakResponse>("/streak")
            .onSuccess { response ->
                logNetwork("StreakRemoteDataSource", "Streak retrieved: ${response.currentStreak}")
            }

    suspend fun recordActivity(): Result<StreakResponse> =
        apiClient.postNotNull<StreakResponse>("/streak/record")
            .onSuccess { response ->
                logNetwork("StreakRemoteDataSource", "Activity recorded: streak=${response.currentStreak}")
            }
            .onFailure { error ->
                logNetwork("StreakRemoteDataSource", "Error recording activity: ${error.message}")
            }
}

