package domain.profile.repository

import domain.common.Try
import domain.profile.model.ProfileStats

interface IProfileStatsRepository {
    suspend fun getProfileStats(): Try<ProfileStats>
}
