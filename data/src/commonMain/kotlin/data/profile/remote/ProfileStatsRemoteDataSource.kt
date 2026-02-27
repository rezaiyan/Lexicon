package data.profile.remote

import data.core.network.client.ApiClient
import data.profile.remote.model.ProfileStatsResponse
import domain.common.Try
import domain.common.doOnSuccess
import expects.logNetwork

class ProfileStatsRemoteDataSource(
    private val apiClient: ApiClient
) {

    suspend fun getProfileStats(): Try<ProfileStatsResponse> =
        apiClient.getNotNull<ProfileStatsResponse>("/users/profile-stats")
            .doOnSuccess { response ->
                logNetwork(
                    "ProfileStatsRemoteDataSource",
                    "Profile stats retrieved: streak=${response.currentStreak}, longest=${response.longestStreak}"
                )
            }
}
