package presentation.feature.leaderboard

import core.common.Try
import domain.leaderboard.model.Leaderboard
import domain.leaderboard.model.LeaderboardEntry
import domain.leaderboard.usecase.GetLeaderboardUseCase
import domain.leaderboard.repository.ILeaderboardRepository
import fakes.FakeAnalyticsTracker
import feature.leaderboard.LeaderboardViewModel
import feature.leaderboard.model.LeaderboardUiData
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import core.common.UiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LeaderboardViewModelTest : ViewModelTestBase() {

    private fun createFakeUseCase(result: Try<Leaderboard>): GetLeaderboardUseCase {
        val repo = object : ILeaderboardRepository {
            override suspend fun getLeaderboard(): Try<Leaderboard> = result
        }
        return GetLeaderboardUseCase(repo)
    }

    private fun testLeaderboard() = Leaderboard(
        entries = listOf(
            LeaderboardEntry(
                rank = 1,
                displayName = "Alice",
                currentStreak = 10,
                longestStreak = 20,
                masteredWords = 50,
                isCurrentUser = false
            ),
            LeaderboardEntry(
                rank = 2,
                displayName = "Bob",
                currentStreak = 5,
                longestStreak = 15,
                masteredWords = 30,
                isCurrentUser = true
            )
        ),
        userEntry = LeaderboardEntry(
            rank = 2,
            displayName = "Bob",
            currentStreak = 5,
            longestStreak = 15,
            masteredWords = 30,
            isCurrentUser = true
        )
    )

    @Test
    fun `successful load maps to Loaded state`() = runTest {
        val leaderboard = testLeaderboard()
        val vm = LeaderboardViewModel(createFakeUseCase(Try.success(leaderboard)), FakeAnalyticsTracker())

        val state = assertIs<UiState.Loaded<*>>(vm.currentState)
        val data = state.value as LeaderboardUiData
        assertEquals(2, data.entries.size)
        assertEquals("Alice", data.entries[0].displayName)
        assertEquals("Bob", data.userEntry?.displayName)
    }

    @Test
    fun `failed load maps to Error state`() = runTest {
        val vm = LeaderboardViewModel(
            createFakeUseCase(Try.failure(RuntimeException("Network error"))),
            FakeAnalyticsTracker(),
        )

        val state = assertIs<UiState.Error>(vm.currentState)
        assertEquals("Network error", state.message)
    }

    @Test
    fun `refresh reloads leaderboard`() = runTest {
        var callCount = 0
        val leaderboard = testLeaderboard()
        val repo = object : ILeaderboardRepository {
            override suspend fun getLeaderboard(): Try<Leaderboard> {
                callCount++
                return Try.success(leaderboard)
            }
        }
        val vm = LeaderboardViewModel(GetLeaderboardUseCase(repo), FakeAnalyticsTracker())

        assertEquals(1, callCount)
        vm.refresh()
        assertEquals(2, callCount)
        assertIs<UiState.Loaded<*>>(vm.currentState)
    }
}
