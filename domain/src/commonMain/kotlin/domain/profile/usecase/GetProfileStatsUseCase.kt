package domain.profile.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.profile.model.ProfileStats
import domain.profile.repository.IProfileStatsRepository

class GetProfileStatsUseCase(
    private val repository: IProfileStatsRepository
) : NoParamUseCase<ProfileStats> {
    suspend operator fun invoke(): Try<ProfileStats> = repository.getProfileStats()

    override suspend operator fun invoke(params: Unit) = invoke()
}
