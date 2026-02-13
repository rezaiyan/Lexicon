package domain.streak.repository

import domain.common.Try
import domain.streak.model.StreakData

interface IStreakRepository {
    suspend fun getStreak(): Try<StreakData>
    suspend fun recordActivity(): Try<StreakData>
}



