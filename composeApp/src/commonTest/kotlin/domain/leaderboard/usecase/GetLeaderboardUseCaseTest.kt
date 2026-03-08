package domain.leaderboard.usecase

import core.common.Try
import core.common.getOrThrow
import domain.leaderboard.model.Leaderboard
import domain.leaderboard.model.LeaderboardEntry
import domain.leaderboard.repository.ILeaderboardRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetLeaderboardUseCaseTest {

    private val repository = FakeLeaderboardRepository()
    private val useCase = GetLeaderboardUseCase(repository)

    @Test
    fun `returns leaderboard on success`() = runTest {
        val entries = listOf(
            LeaderboardEntry(1, "Alice", 10, 20, 100, false),
            LeaderboardEntry(2, "Bob", 5, 15, 80, true)
        )
        val expected = Leaderboard(entries, entries[1])
        repository.result = Try.success(expected)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().entries.size)
        assertEquals("Bob", result.getOrThrow().userEntry?.displayName)
    }

    @Test
    fun `returns empty leaderboard`() = runTest {
        repository.result = Try.success(Leaderboard(emptyList(), null))

        val result = useCase()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().entries.isEmpty())
        assertEquals(null, result.getOrThrow().userEntry)
    }

    @Test
    fun `returns failure on repository error`() = runTest {
        repository.result = Try.failure(RuntimeException("Network error"))

        val result = useCase()

        assertTrue(result.isFailure)
    }

    @Test
    fun `invoke with Unit params delegates correctly`() = runTest {
        repository.result = Try.success(Leaderboard(emptyList(), null))

        val result = useCase(Unit)

        assertTrue(result.isSuccess)
    }
}

private class FakeLeaderboardRepository : ILeaderboardRepository {
    var result: Try<Leaderboard> = Try.success(Leaderboard(emptyList(), null))

    override suspend fun getLeaderboard(): Try<Leaderboard> = result
}
