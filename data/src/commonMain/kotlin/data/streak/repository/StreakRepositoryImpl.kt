package data.streak.repository

import data.streak.remote.IStreakRemoteDataSource
import core.common.Try
import core.common.fold
import domain.streak.model.StreakData
import domain.streak.repository.IStreakRepository

class StreakRepositoryImpl(
    private val streakRemoteDataSource: IStreakRemoteDataSource
) : IStreakRepository {

    override suspend fun getStreak(): Try<StreakData> {
        return streakRemoteDataSource.getStreak().fold(
            onSuccess = { streakResponse ->
                val streakData = StreakData(
                    currentStreak = streakResponse.currentStreak
                )
                Try.success(streakData)
            },
            onFailure = { error ->
                Try.failure(error)
            }
        )
    }

    override suspend fun recordActivity(count: Int): Try<StreakData> {
        return streakRemoteDataSource.recordActivity(count).fold(
            onSuccess = { streakResponse ->
                val streakData = StreakData(
                    currentStreak = streakResponse.currentStreak
                )
                Try.success(streakData)
            },
            onFailure = { error ->
                Try.failure(error)
            }
        )
    }
}

