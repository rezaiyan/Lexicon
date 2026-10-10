package domain.insights

import app.cash.turbine.test
import core.common.Try
import domain.insights.usecase.DismissCoachCardUseCase
import domain.insights.usecase.ObserveDismissedCoachCardsUseCase
import domain.insights.usecase.ObserveInsightsScreenUseCase
import domain.insights.usecase.RefreshInsightsScreenUseCase
import fakes.FakeDismissedCoachCardsRepository
import fakes.FakeInsightsScreenRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InsightsScreenUseCasesTest {

    @Test
    fun `refresh forwards the time zone and observe emits the cache`() = runTest {
        val repo = FakeInsightsScreenRepository()
        ObserveInsightsScreenUseCase(repo)(Unit).test {
            assertNull(awaitItem())
            repo.nextRefresh = InsightsFixtures.cached()
            assertTrue(RefreshInsightsScreenUseCase(repo)("Europe/Berlin").isSuccess)
            assertEquals(InsightsFixtures.cached(), awaitItem())
        }
        assertEquals(listOf("Europe/Berlin"), repo.refreshedZones)
    }

    @Test
    fun `dismissed ids flow through`() = runTest {
        val repo = FakeDismissedCoachCardsRepository()
        DismissCoachCardUseCase(repo)("WEEK_TREND:2026-10-10")
        ObserveDismissedCoachCardsUseCase(repo)(Unit).test {
            assertEquals(setOf("WEEK_TREND:2026-10-10"), awaitItem())
        }
    }
}
