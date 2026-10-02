package domain.subscription.usecase

import core.common.Try
import fakes.FakeSubscriptionAccessRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RefreshFeatureAccessUseCaseTest {

    private val repository = FakeSubscriptionAccessRepository()
    private val useCase = RefreshFeatureAccessUseCase(repository)

    @Test
    fun `invoke delegates to repository refresh`() = runTest {
        assertTrue(useCase().isSuccess)

        assertEquals(1, repository.refreshCount)
        assertEquals(0, repository.syncCount)
    }

    @Test
    fun `invoke when repository fails returns failure`() = runTest {
        repository.result = Try.failure(RuntimeException("offline"))

        assertFalse(useCase(Unit).isSuccess)
    }
}
