package domain.profile.repository

import core.common.Try
import domain.profile.model.ProfileStats

interface IProfileStatsRepository {
    suspend fun getProfileStats(): Try<ProfileStats>
}
