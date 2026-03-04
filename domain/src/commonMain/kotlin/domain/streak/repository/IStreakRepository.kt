package domain.streak.repository

import core.common.Try
import domain.streak.model.StreakData

interface IStreakRepository {
    suspend fun getStreak(): Try<StreakData>
    suspend fun recordActivity(count: Int): Try<StreakData>
}



