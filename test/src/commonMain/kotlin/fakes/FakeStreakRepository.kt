package fakes

import core.common.Try
import domain.streak.model.StreakData
import domain.streak.repository.IStreakRepository

class FakeStreakRepository : IStreakRepository {
    var streakResult: Try<StreakData> = Try.success(StreakData(currentStreak = 0))
    var recordResult: Try<StreakData> = Try.success(StreakData(currentStreak = 1))
    var lastRecordedCount: Int? = null

    override suspend fun getStreak(): Try<StreakData> = streakResult
    override suspend fun recordActivity(count: Int): Try<StreakData> {
        lastRecordedCount = count
        return recordResult
    }
}
