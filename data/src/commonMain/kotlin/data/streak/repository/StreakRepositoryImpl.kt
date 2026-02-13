package data.streak.repository

import data.streak.remote.StreakRemoteDataSource
import domain.common.Try
import domain.common.fold
import domain.streak.model.StreakData
import domain.streak.repository.IStreakRepository

class StreakRepositoryImpl(
    private val streakRemoteDataSource: StreakRemoteDataSource
) : IStreakRepository {

    override suspend fun getStreak(): Try<StreakData> {
        return streakRemoteDataSource.getStreak().fold(
            onSuccess = { streakResponse ->
                val streakData = StreakData(
                    currentStreak = streakResponse.currentStreak,
                    highestStreak = streakResponse.longestStreak
                )
                Try.success(streakData)
            },
            onFailure = { error ->
                Try.failure(error)
            }
        )
    }

    override suspend fun recordActivity(): Try<StreakData> {
        return streakRemoteDataSource.recordActivity().fold(
            onSuccess = { streakResponse ->
                val streakData = StreakData(
                    currentStreak = streakResponse.currentStreak,
                    highestStreak = streakResponse.longestStreak
                )
                Try.success(streakData)
            },
            onFailure = { error ->
                Try.failure(error)
            }
        )
    }
}

