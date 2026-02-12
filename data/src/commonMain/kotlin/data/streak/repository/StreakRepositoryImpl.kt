package data.streak.repository

import data.streak.remote.StreakRemoteDataSource
import domain.streak.model.StreakData
import domain.streak.repository.IStreakRepository

class StreakRepositoryImpl(
    private val streakRemoteDataSource: StreakRemoteDataSource
) : IStreakRepository {

    override suspend fun getStreak(): Result<StreakData> {
        return streakRemoteDataSource.getStreak().fold(
            onSuccess = { streakResponse ->
                val streakData = StreakData(
                    currentStreak = streakResponse.currentStreak,
                    highestStreak = streakResponse.longestStreak
                )
                Result.success(streakData)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    override suspend fun recordActivity(): Result<StreakData> {
        return streakRemoteDataSource.recordActivity().fold(
            onSuccess = { streakResponse ->
                val streakData = StreakData(
                    currentStreak = streakResponse.currentStreak,
                    highestStreak = streakResponse.longestStreak
                )
                Result.success(streakData)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
}

