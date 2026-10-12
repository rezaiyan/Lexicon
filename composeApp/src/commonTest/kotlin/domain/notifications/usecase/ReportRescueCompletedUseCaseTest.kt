package domain.notifications.usecase

import core.common.Try
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportRescueCompletedUseCaseTest {

    private val repository = FakeNotificationEngagementRepository()
    private val useCase = ReportRescueCompletedUseCase(repository)

    @Test
    fun `invoke reports the rescue id`() = runTest {
        val result = useCase(42L)

        assertTrue(result.isSuccess)
        assertEquals(listOf(42L), repository.completedRescues)
    }

    @Test
    fun `invoke when repository fails returns failure`() = runTest {
        repository.result = Try.failure(RuntimeException("offline"))

        assertTrue(useCase(42L).isFailure)
    }
}
