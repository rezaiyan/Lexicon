package data.profile.remote

import data.profile.remote.model.ProfileStatsResponse
import core.common.Try

interface IProfileStatsRemoteDataSource {
    suspend fun getProfileStats(): Try<ProfileStatsResponse>
}
