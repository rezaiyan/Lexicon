package fakes

import core.common.Try
import domain.profile.model.ProfileStats
import domain.profile.repository.IProfileStatsRepository

class FakeProfileStatsRepository : IProfileStatsRepository {
    var stats: Try<ProfileStats> = Try.success(ProfileStats(0, 0, "", emptyList(), emptyList()))

    override suspend fun getProfileStats(): Try<ProfileStats> = stats
}
