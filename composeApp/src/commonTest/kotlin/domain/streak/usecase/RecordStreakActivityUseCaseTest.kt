package domain.streak.usecase

import core.common.Try
import core.common.getOrThrow
import domain.streak.model.StreakData
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordStreakActivityUseCaseTest {

    private val repository = FakeStreakRepository()
    private val useCase = RecordStreakActivityUseCase(repository)

    @Test
    fun `records activity and returns updated streak`() = runTest {
        repository.recordResult = Try.success(StreakData(currentStreak = 5))

        val result = useCase(10)

        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrThrow().currentStreak)
        assertEquals(10, repository.lastRecordedCount)
    }

    @Test
    fun `passes count to repository`() = runTest {
        useCase(25)

        assertEquals(25, repository.lastRecordedCount)
    }

    @Test
    fun `returns failure on repository error`() = runTest {
        repository.recordResult = Try.failure(RuntimeException("Network error"))

        val result = useCase(5)

        assertTrue(result.isFailure)
    }
}
