package data.insights

import app.cash.turbine.test
import data.insights.repository.DismissedCoachCardsRepositoryImpl
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DismissedCoachCardsRepositoryImplTest {

    @Test
    fun `dismiss when called records card and drops other days`() = runTest {
        val local = FakeInsightsScreenLocalDataSource().apply {
            dismissed.value = setOf("streak:2026-10-09", "review:2026-10-10")
        }
        val repo = DismissedCoachCardsRepositoryImpl(local, today = { "2026-10-10" })

        assertTrue(repo.dismiss("rush:2026-10-10").isSuccess)

        repo.observe().test {
            assertEquals(setOf("review:2026-10-10", "rush:2026-10-10"), awaitItem())
        }
    }
}
