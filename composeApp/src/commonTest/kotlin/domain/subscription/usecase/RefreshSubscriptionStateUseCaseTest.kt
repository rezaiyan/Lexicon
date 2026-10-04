package domain.subscription.usecase

import core.common.Try
import fakes.FakeSubscriptionAccessRepository
import fakes.FakeSubscriptionManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RefreshSubscriptionStateUseCaseTest {

    private val accessRepository = FakeSubscriptionAccessRepository()
    private val subscriptionManager = FakeSubscriptionManager()
    private val useCase = RefreshSubscriptionStateUseCase(accessRepository, subscriptionManager)

    @Test
    fun `invoke force refreshes backend access and store status`() = runTest {
        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(1, accessRepository.forcedRefreshCount)
        assertEquals(1, subscriptionManager.refreshCount)
    }

    @Test
    fun `invoke when backend fails still refreshes store and reports failure`() = runTest {
        accessRepository.result = Try.failure(IllegalStateException("backend down"))

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(1, subscriptionManager.refreshCount)
    }

    @Test
    fun `invoke when store fails reports failure`() = runTest {
        subscriptionManager.refreshResult = Try.failure(IllegalStateException("store down"))

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(1, accessRepository.forcedRefreshCount)
    }
}
