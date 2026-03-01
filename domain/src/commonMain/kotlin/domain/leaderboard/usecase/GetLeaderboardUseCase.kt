package domain.leaderboard.usecase

import domain.common.Try
import domain.leaderboard.model.Leaderboard
import domain.leaderboard.repository.ILeaderboardRepository

class GetLeaderboardUseCase(
    private val repository: ILeaderboardRepository
) {
    suspend operator fun invoke(): Try<Leaderboard> = repository.getLeaderboard()
}
