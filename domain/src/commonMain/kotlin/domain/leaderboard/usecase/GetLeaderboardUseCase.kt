package domain.leaderboard.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.leaderboard.model.Leaderboard
import domain.leaderboard.repository.ILeaderboardRepository

class GetLeaderboardUseCase(
    private val repository: ILeaderboardRepository
) : NoParamUseCase<Leaderboard> {
    suspend operator fun invoke(): Try<Leaderboard> = repository.getLeaderboard()

    override suspend operator fun invoke(params: Unit) = invoke()
}
