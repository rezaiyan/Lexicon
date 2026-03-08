package domain.profile.usecase

import core.common.Try
import core.common.getOrThrow
import domain.profile.model.DayActivity
import domain.profile.model.LanguagePair
import domain.profile.model.ProfileStats
import domain.profile.repository.IProfileStatsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetProfileStatsUseCaseTest {

    private val repository = FakeProfileStatsRepository()
    private val useCase = GetProfileStatsUseCase(repository)

    @Test
    fun `returns profile stats on success`() = runTest {
        val expected = ProfileStats(
            currentStreak = 5,
            longestStreak = 10,
            memberSince = "2024-01-01",
            weeklyActivity = listOf(DayActivity("2024-03-01", 10)),
            languages = listOf(LanguagePair("en", "de", 50))
        )
        repository.stats = Try.success(expected)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrThrow())
    }

    @Test
    fun `returns failure when repository fails`() = runTest {
        repository.stats = Try.failure(RuntimeException("Network error"))

        val result = useCase()

        assertTrue(result.isFailure)
    }

    @Test
    fun `invoke with Unit params delegates correctly`() = runTest {
        val expected = ProfileStats(
            currentStreak = 3,
            longestStreak = 7,
            memberSince = "2024-06-01",
            weeklyActivity = emptyList(),
            languages = emptyList()
        )
        repository.stats = Try.success(expected)

        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().currentStreak)
    }
}

private class FakeProfileStatsRepository : IProfileStatsRepository {
    var stats: Try<ProfileStats> = Try.success(
        ProfileStats(0, 0, "", emptyList(), emptyList())
    )

    override suspend fun getProfileStats(): Try<ProfileStats> = stats
}
