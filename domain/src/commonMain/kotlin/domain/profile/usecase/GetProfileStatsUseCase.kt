package domain.profile.usecase

import core.common.Try
import domain.profile.model.ProfileStats
import domain.profile.repository.IProfileStatsRepository

class GetProfileStatsUseCase(
    private val repository: IProfileStatsRepository
) {
    suspend operator fun invoke(): Try<ProfileStats> = repository.getProfileStats()
}
