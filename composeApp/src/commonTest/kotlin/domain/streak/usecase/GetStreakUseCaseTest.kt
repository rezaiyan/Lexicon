package domain.streak.usecase

import core.common.Try
import core.common.getOrThrow
import domain.streak.model.StreakData
import domain.streak.repository.IStreakRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetStreakUseCaseTest {

    private val repository = FakeStreakRepository()
    private val useCase = GetStreakUseCase(repository)

    @Test
    fun `returns streak data on success`() = runTest {
        repository.streakResult = Try.success(StreakData(currentStreak = 7))

        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertEquals(7, result.getOrThrow().currentStreak)
    }

    @Test
    fun `returns failure on repository error`() = runTest {
        repository.streakResult = Try.failure(RuntimeException("Network error"))

        val result = useCase(Unit)

        assertTrue(result.isFailure)
    }

    @Test
    fun `returns zero streak by default`() = runTest {
        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().currentStreak)
    }
}

internal class FakeStreakRepository : IStreakRepository {
    var streakResult: Try<StreakData> = Try.success(StreakData(currentStreak = 0))
    var recordResult: Try<StreakData> = Try.success(StreakData(currentStreak = 1))
    var lastRecordedCount: Int? = null

    override suspend fun getStreak(): Try<StreakData> = streakResult
    override suspend fun recordActivity(count: Int): Try<StreakData> {
        lastRecordedCount = count
        return recordResult
    }
}
