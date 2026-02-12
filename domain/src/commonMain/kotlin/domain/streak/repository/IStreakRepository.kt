package domain.streak.repository

import domain.streak.model.StreakData

interface IStreakRepository {
    suspend fun getStreak(): Result<StreakData>
    suspend fun recordActivity(): Result<StreakData>
}



