package domain.notifications.usecase

import core.common.Try
import domain.notifications.repository.INotificationEngagementRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportNotificationOpenedUseCaseTest {

    private val repository = FakeNotificationEngagementRepository()
    private val useCase = ReportNotificationOpenedUseCase(repository)

    @Test
    fun `invoke when payload carries a log id reports it`() = runTest {
        val result = useCase(mapOf("type" to "due_cards", "notification_log_id" to "42"))

        assertTrue(result.isSuccess)
        assertEquals(listOf(42L), repository.reported)
    }

    @Test
    fun `invoke when payload has no log id succeeds without reporting`() = runTest {
        val result = useCase(mapOf("type" to "review_reminder"))

        assertTrue(result.isSuccess)
        assertEquals(emptyList(), repository.reported)
    }

    @Test
    fun `invoke when log id is not a number succeeds without reporting`() = runTest {
        val result = useCase(mapOf("notification_log_id" to "abc"))

        assertTrue(result.isSuccess)
        assertEquals(emptyList(), repository.reported)
    }

    @Test
    fun `invoke when repository fails returns failure`() = runTest {
        repository.result = Try.failure(RuntimeException("offline"))

        val result = useCase(mapOf("notification_log_id" to "42"))

        assertTrue(result.isFailure)
    }
}

internal class FakeNotificationEngagementRepository : INotificationEngagementRepository {
    val reported = mutableListOf<Long>()
    var result: Try<Unit> = Try.success(Unit)

    override suspend fun reportOpened(notificationLogId: Long): Try<Unit> {
        reported += notificationLogId
        return result
    }
}
