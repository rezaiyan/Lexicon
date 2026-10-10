package data.insights

import app.cash.turbine.test
import core.common.Try
import data.insights.local.StoredInsightsScreen
import data.insights.remote.HeroDto
import data.insights.remote.IInsightsScreenRemoteDataSource
import data.insights.remote.InsightsScreenDto
import data.insights.repository.InsightsScreenRepositoryImpl
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InsightsScreenRepositoryImplTest {

    private class FakeRemote(var result: Try<InsightsScreenDto>) : IInsightsScreenRemoteDataSource {
        val zones = mutableListOf<String>()
        override suspend fun fetch(timeZoneId: String): Try<InsightsScreenDto> {
            zones += timeZoneId
            return result
        }
    }

    private val dto = InsightsScreenDto(totalReviews = 7L, hero = HeroDto(headline = "Hi"))

    @Test
    fun `refresh when remote succeeds writes cache with fetch time`() = runTest {
        val local = FakeInsightsScreenLocalDataSource()
        val remote = FakeRemote(Try.success(dto))
        val repo = InsightsScreenRepositoryImpl(remote, local, nowMs = { 42L })

        repo.observe().test {
            assertNull(awaitItem())
            assertTrue(repo.refresh("Europe/Berlin").isSuccess)
            val cached = awaitItem()
            assertEquals(7L, cached?.screen?.totalReviews)
            assertEquals(42L, cached?.fetchedAtMs)
        }
        assertEquals(listOf("Europe/Berlin"), remote.zones)
    }

    @Test
    fun `refresh when remote fails keeps the cache`() = runTest {
        val local = FakeInsightsScreenLocalDataSource().apply { stored.value = StoredInsightsScreen(dto, 1L) }
        val repo = InsightsScreenRepositoryImpl(FakeRemote(Try.failure(Exception("offline"))), local, nowMs = { 99L })

        assertTrue(repo.refresh("UTC").isFailure)
        repo.observe().test { assertEquals(1L, awaitItem()?.fetchedAtMs) }
    }
}
