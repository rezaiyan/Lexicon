package data.streak.remote

import data.streak.remote.model.StreakResponse
import core.common.Try

interface IStreakRemoteDataSource {
    suspend fun getStreak(): Try<StreakResponse>
    suspend fun recordActivity(count: Int): Try<StreakResponse>
}
