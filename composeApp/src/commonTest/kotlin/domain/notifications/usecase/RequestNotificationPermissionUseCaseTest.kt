package domain.notifications.usecase

import core.common.getOrThrow
import domain.notifications.repository.INotificationRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RequestNotificationPermissionUseCaseTest {

    private val repository = FakeNotificationRepository()
    private val useCase = RequestNotificationPermissionUseCase(repository)

    @Test
    fun `returns true when permission granted`() = runTest {
        repository.permissionGranted = true

        val result = useCase()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun `returns false when permission denied`() = runTest {
        repository.permissionGranted = false

        val result = useCase()

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
    }

    @Test
    fun `invoke with Unit params delegates correctly`() = runTest {
        repository.permissionGranted = true

        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun `returns failure on error`() = runTest {
        repository.shouldThrow = true

        val result = useCase()

        assertTrue(result.isFailure)
    }
}

internal class FakeNotificationRepository : INotificationRepository {
    var permissionGranted = true
    var shouldThrow = false
    var scheduledReminder = false
    var lastScheduledDueCount: Int? = null
    var lastScheduledTitle: String? = null
    var lastScheduledMessage: String? = null
    var lastScheduledDelayMinutes: Int? = null
    var openSettingsCalled = false

    override suspend fun requestNotificationPermission(): Boolean {
        if (shouldThrow) throw RuntimeException("Permission error")
        return permissionGranted
    }

    override suspend fun areNotificationsEnabled(): Boolean = true
    override suspend fun wasNotificationPermissionDenied(): Boolean = false

    override suspend fun scheduleReviewReminder(dueCount: Int, title: String, message: String, delayMinutes: Int) {
        scheduledReminder = true
        lastScheduledDueCount = dueCount
        lastScheduledTitle = title
        lastScheduledMessage = message
        lastScheduledDelayMinutes = delayMinutes
    }

    override suspend fun openNotificationSettings() {
        if (shouldThrow) throw RuntimeException("Settings error")
        openSettingsCalled = true
    }
}
