package data.leaderboard.repository

import core.common.Try
import core.common.getOrThrow
import data.leaderboard.remote.ILeaderboardRemoteDataSource
import data.leaderboard.remote.model.LeaderboardEntryResponse
import data.leaderboard.remote.model.LeaderboardResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LeaderboardRepositoryImplTest {

    private val remoteDataSource = FakeLeaderboardRemoteDataSource()

    private fun createRepo() = LeaderboardRepositoryImpl(remoteDataSource)

    @Test
    fun `getLeaderboard maps entries correctly`() = runTest {
        remoteDataSource.result = Try.success(
            LeaderboardResponse(
                entries = listOf(
                    LeaderboardEntryResponse(
                        rank = 1, displayName = "Alice", currentStreak = 10,
                        longestStreak = 20, masteredWords = 100, isCurrentUser = false,
                        profileImageUrl = "https://img.example.com/alice.jpg"
                    ),
                    LeaderboardEntryResponse(
                        rank = 2, displayName = "Bob", currentStreak = 5,
                        longestStreak = 15, masteredWords = 50, isCurrentUser = true
                    )
                ),
                userEntry = LeaderboardEntryResponse(
                    rank = 2, displayName = "Bob", currentStreak = 5,
                    longestStreak = 15, masteredWords = 50, isCurrentUser = true
                )
            )
        )
        val repo = createRepo()

        val result = repo.getLeaderboard()

        assertTrue(result.isSuccess)
        val leaderboard = result.getOrThrow()
        assertEquals(2, leaderboard.entries.size)
        assertEquals("Alice", leaderboard.entries[0].displayName)
        assertEquals(1, leaderboard.entries[0].rank)
        assertEquals(100, leaderboard.entries[0].masteredWords)
        assertEquals("https://img.example.com/alice.jpg", leaderboard.entries[0].profileImageUrl)
        assertTrue(leaderboard.entries[1].isCurrentUser)
        assertEquals("Bob", leaderboard.userEntry?.displayName)
    }

    @Test
    fun `getLeaderboard handles null userEntry`() = runTest {
        remoteDataSource.result = Try.success(
            LeaderboardResponse(entries = emptyList(), userEntry = null)
        )
        val repo = createRepo()

        val result = repo.getLeaderboard()

        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow().userEntry)
    }

    @Test
    fun `getLeaderboard returns failure on error`() = runTest {
        remoteDataSource.result = Try.failure(RuntimeException("Network error"))
        val repo = createRepo()

        val result = repo.getLeaderboard()

        assertTrue(result.isFailure)
    }

    // --- Fakes ---

    private class FakeLeaderboardRemoteDataSource : ILeaderboardRemoteDataSource {
        var result: Try<LeaderboardResponse> = Try.failure(RuntimeException("not set"))

        override suspend fun getLeaderboard(): Try<LeaderboardResponse> = result
    }
}
