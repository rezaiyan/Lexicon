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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LeaderboardViewModelTest : ViewModelTestBase() {

    private class SequenceRepository(vararg results: Try<Leaderboard>) : ILeaderboardRepository {
        private val queue = ArrayDeque(results.toList())
        var callCount = 0
            private set

        override suspend fun getLeaderboard(): Try<Leaderboard> {
            callCount++
            return if (queue.size > 1) queue.removeFirst() else queue.first()
        }
    }

    private fun createViewModel(repository: ILeaderboardRepository) =
        LeaderboardViewModel(GetLeaderboardUseCase(repository), FakeAnalyticsTracker())

    private fun entry(rank: Int, name: String, isCurrentUser: Boolean = false) = LeaderboardEntry(
        rank = rank,
        displayName = name,
        currentStreak = 10 - rank,
        longestStreak = 20 - rank,
        masteredWords = 100 - rank * 10,
        isCurrentUser = isCurrentUser
    )

    private fun leaderboard(size: Int, userRank: Int?): Leaderboard {
        val entries = (1..size).map { rank -> entry(rank, "User $rank", isCurrentUser = rank == userRank) }
        return Leaderboard(
            entries = entries,
            userEntry = userRank?.let { entry(it, "User $it", isCurrentUser = true) }
        )
    }

    private fun LeaderboardViewModel.loadedData(): LeaderboardUiData {
        val content = assertIs<UiState.Loaded<*>>(currentState.content)
        return assertIs<LeaderboardUiData>(content.value)
    }

    @Test
    fun `load with more than three entries splits podium and rows`() = runTest {
        val vm = createViewModel(SequenceRepository(Try.success(leaderboard(size = 6, userRank = 5))))

        val data = vm.loadedData()
        assertEquals(listOf(1, 2, 3), data.podium.map { it.rank })
        assertEquals(listOf(4, 5, 6), data.rows.map { it.rank })
        assertEquals(1, data.userRowIndex)
        assertEquals(5, data.userEntry?.rank)
    }

    @Test
    fun `load with fewer than three entries skips podium`() = runTest {
        val vm = createViewModel(SequenceRepository(Try.success(leaderboard(size = 2, userRank = 2))))

        val data = vm.loadedData()
        assertTrue(data.podium.isEmpty())
        assertEquals(listOf(1, 2), data.rows.map { it.rank })
        assertEquals(1, data.userRowIndex)
    }

    @Test
    fun `load when user is on podium has no row index`() = runTest {
        val vm = createViewModel(SequenceRepository(Try.success(leaderboard(size = 5, userRank = 2))))

        val data = vm.loadedData()
        assertNull(data.userRowIndex)
        assertTrue(data.podium[1].isCurrentUser)
    }

    @Test
    fun `load when user is unranked has no user entry`() = runTest {
        val vm = createViewModel(SequenceRepository(Try.success(leaderboard(size = 4, userRank = null))))

        val data = vm.loadedData()
        assertNull(data.userEntry)
        assertNull(data.userRowIndex)
    }

    @Test
    fun `load with unsorted entries orders them by rank`() = runTest {
        val unsorted = Leaderboard(
            entries = listOf(entry(3, "C"), entry(1, "A"), entry(4, "D"), entry(2, "B")),
            userEntry = null
        )
        val vm = createViewModel(SequenceRepository(Try.success(unsorted)))

        val data = vm.loadedData()
        assertEquals(listOf("A", "B", "C"), data.podium.map { it.displayName })
        assertEquals(listOf("D"), data.rows.map { it.displayName })
    }

    @Test
    fun `load sets nextUp to the entry directly above the user`() = runTest {
        val vm = createViewModel(SequenceRepository(Try.success(leaderboard(size = 6, userRank = 5))))

        assertEquals(4, vm.loadedData().nextUp?.rank)
    }

    @Test
    fun `load when user leads has no nextUp`() = runTest {
        val vm = createViewModel(SequenceRepository(Try.success(leaderboard(size = 6, userRank = 1))))

        assertNull(vm.loadedData().nextUp)
    }

    @Test
    fun `load when user ties skips tied entries for nextUp`() = runTest {
        val tied = Leaderboard(
            entries = listOf(
                entry(1, "A"),
                entry(2, "B"),
                entry(3, "C"),
                entry(3, "Me", isCurrentUser = true)
            ),
            userEntry = entry(3, "Me", isCurrentUser = true)
        )
        val vm = createViewModel(SequenceRepository(Try.success(tied)))

        assertEquals("B", vm.loadedData().nextUp?.displayName)
    }

    @Test
    fun `load when user is far beyond the list has no nextUp`() = runTest {
        val board = Leaderboard(
            entries = (1..4).map { entry(it, "User $it") },
            userEntry = entry(40, "Me", isCurrentUser = true)
        )
        val vm = createViewModel(SequenceRepository(Try.success(board)))

        val data = vm.loadedData()
        assertNull(data.userRowIndex)
        assertNull(data.nextUp)
    }

    @Test
    fun `load when user is just below the list gets last entry as nextUp`() = runTest {
        val board = Leaderboard(
            entries = (1..4).map { entry(it, "User $it") },
            userEntry = entry(5, "Me", isCurrentUser = true)
        )
        val vm = createViewModel(SequenceRepository(Try.success(board)))

        assertEquals(4, vm.loadedData().nextUp?.rank)
    }

    @Test
    fun `load with no entries is empty`() = runTest {
        val vm = createViewModel(SequenceRepository(Try.success(Leaderboard(emptyList(), null))))

        assertTrue(vm.loadedData().isEmpty)
    }

    @Test
    fun `initial load failure maps to Error state`() = runTest {
        val vm = createViewModel(SequenceRepository(Try.failure(RuntimeException("Network error"))))

        val content = assertIs<UiState.Error>(vm.currentState.content)
        assertEquals("Network error", content.message)
    }

    @Test
    fun `refresh reloads leaderboard`() = runTest {
        val repository = SequenceRepository(Try.success(leaderboard(size = 4, userRank = 4)))
        val vm = createViewModel(repository)

        assertEquals(1, repository.callCount)
        vm.refresh()
        assertEquals(2, repository.callCount)
        assertIs<UiState.Loaded<*>>(vm.currentState.content)
        assertFalse(vm.currentState.isRefreshing)
    }

    @Test
    fun `refresh failure keeps previous content and flags refresh error`() = runTest {
        val repository = SequenceRepository(
            Try.success(leaderboard(size = 4, userRank = 4)),
            Try.failure(RuntimeException("Network error"))
        )
        val vm = createViewModel(repository)

        vm.refresh()

        assertEquals(4, vm.loadedData().rows.size + vm.loadedData().podium.size)
        assertTrue(vm.currentState.refreshFailed)
        assertFalse(vm.currentState.isRefreshing)
    }

    @Test
    fun `refresh success after failure clears refresh error`() = runTest {
        val repository = SequenceRepository(
            Try.success(leaderboard(size = 4, userRank = 4)),
            Try.failure(RuntimeException("Network error")),
            Try.success(leaderboard(size = 5, userRank = 5))
        )
        val vm = createViewModel(repository)

        vm.refresh()
        vm.refresh()

        assertFalse(vm.currentState.refreshFailed)
        assertEquals(2, vm.loadedData().rows.size)
    }

    @Test
    fun `retry after initial failure loads content`() = runTest {
        val repository = SequenceRepository(
            Try.failure(RuntimeException("Network error")),
            Try.success(leaderboard(size = 3, userRank = 1))
        )
        val vm = createViewModel(repository)

        vm.refresh()

        assertEquals(3, vm.loadedData().podium.size)
        assertFalse(vm.currentState.refreshFailed)
    }
}
