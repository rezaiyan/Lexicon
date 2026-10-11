package domain.support

import domain.support.model.SupportDeliveryStatus
import domain.support.model.SupportMessage
import domain.support.model.SupportSender
import domain.support.usecase.SyncSupportRepliesUseCase
import fakes.FakeSupportRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncSupportRepliesUseCaseTest {

    private val repository = FakeSupportRepository()
    private val useCase = SyncSupportRepliesUseCase(repository)

    @Test
    fun `invoke without a conversation skips the network`() = runTest {
        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertEquals(0, repository.refreshCount)
    }

    @Test
    fun `invoke with a conversation refreshes it`() = runTest {
        repository.messages.value = listOf(
            SupportMessage("c-1", SupportSender.USER, "Hi", 0, SupportDeliveryStatus.SENT),
        )

        useCase(Unit)

        assertEquals(1, repository.refreshCount)
    }
}
