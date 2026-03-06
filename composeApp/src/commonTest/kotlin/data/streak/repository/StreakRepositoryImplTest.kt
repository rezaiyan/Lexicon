package data.streak.repository

import core.common.Try
import core.common.getOrThrow
import data.streak.remote.IStreakRemoteDataSource
import data.streak.remote.model.StreakResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StreakRepositoryImplTest {

    private val remoteDataSource = FakeStreakRemoteDataSource()

    private fun createRepo() = StreakRepositoryImpl(remoteDataSource)

    @Test
    fun `getStreak maps response to StreakData`() = runTest {
        remoteDataSource.streakResult = Try.success(StreakResponse(currentStreak = 7))
        val repo = createRepo()

        val result = repo.getStreak()

        assertTrue(result.isSuccess)
        assertEquals(7, result.getOrThrow().currentStreak)
    }

    @Test
    fun `getStreak returns failure on error`() = runTest {
        remoteDataSource.streakResult = Try.failure(RuntimeException("Network error"))
        val repo = createRepo()

        val result = repo.getStreak()

        assertTrue(result.isFailure)
    }

    @Test
    fun `recordActivity maps response to StreakData`() = runTest {
        remoteDataSource.recordResult = Try.success(StreakResponse(currentStreak = 3))
        val repo = createRepo()

        val result = repo.recordActivity(5)

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().currentStreak)
    }

    @Test
    fun `recordActivity returns failure on error`() = runTest {
        remoteDataSource.recordResult = Try.failure(RuntimeException("Server error"))
        val repo = createRepo()

        val result = repo.recordActivity(1)

        assertTrue(result.isFailure)
    }

    // --- Fakes ---

    private class FakeStreakRemoteDataSource : IStreakRemoteDataSource {
        var streakResult: Try<StreakResponse> = Try.failure(RuntimeException("not set"))
        var recordResult: Try<StreakResponse> = Try.failure(RuntimeException("not set"))

        override suspend fun getStreak(): Try<StreakResponse> = streakResult
        override suspend fun recordActivity(count: Int): Try<StreakResponse> = recordResult
    }
}
