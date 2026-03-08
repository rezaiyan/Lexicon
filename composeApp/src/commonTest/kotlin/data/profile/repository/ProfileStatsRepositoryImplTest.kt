package data.profile.repository

import core.common.Try
import core.common.getOrThrow
import data.profile.remote.IProfileStatsRemoteDataSource
import data.profile.remote.model.DayActivityResponse
import data.profile.remote.model.LanguagePairResponse
import data.profile.remote.model.ProfileStatsResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileStatsRepositoryImplTest {

    private val remoteDataSource = FakeProfileStatsRemoteDataSource()

    private fun createRepo() = ProfileStatsRepositoryImpl(remoteDataSource)

    @Test
    fun `getProfileStats maps response to domain model`() = runTest {
        remoteDataSource.result = Try.success(
            ProfileStatsResponse(
                currentStreak = 5,
                longestStreak = 10,
                memberSince = "2024-01-01",
                weeklyActivity = listOf(
                    DayActivityResponse(date = "2024-03-01", reviewCount = 20)
                ),
                languages = listOf(
                    LanguagePairResponse(sourceLanguage = "en", targetLanguage = "es", wordCount = 50)
                )
            )
        )
        val repo = createRepo()

        val result = repo.getProfileStats()

        assertTrue(result.isSuccess)
        val stats = result.getOrThrow()
        assertEquals(5, stats.currentStreak)
        assertEquals(10, stats.longestStreak)
        assertEquals("2024-01-01", stats.memberSince)
        assertEquals(1, stats.weeklyActivity.size)
        assertEquals("2024-03-01", stats.weeklyActivity.first().date)
        assertEquals(20, stats.weeklyActivity.first().reviewCount)
        assertEquals(1, stats.languages.size)
        assertEquals("en", stats.languages.first().sourceLanguage)
        assertEquals(50, stats.languages.first().wordCount)
    }

    @Test
    fun `getProfileStats returns failure on error`() = runTest {
        remoteDataSource.result = Try.failure(RuntimeException("Server error"))
        val repo = createRepo()

        val result = repo.getProfileStats()

        assertTrue(result.isFailure)
    }

    // --- Fakes ---

    private class FakeProfileStatsRemoteDataSource : IProfileStatsRemoteDataSource {
        var result: Try<ProfileStatsResponse> = Try.failure(RuntimeException("not set"))

        override suspend fun getProfileStats(): Try<ProfileStatsResponse> = result
    }
}
