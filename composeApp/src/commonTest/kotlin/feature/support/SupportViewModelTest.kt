package feature.support

import core.common.Try
import domain.support.model.MAX_SUPPORT_MESSAGE_LENGTH
import domain.support.model.SupportDeliveryStatus
import domain.support.model.SupportMessage
import domain.support.model.SupportSender
import domain.support.model.SupportUnread
import domain.support.usecase.MarkSupportReadUseCase
import domain.support.usecase.ObserveSupportMessagesUseCase
import domain.support.usecase.RefreshSupportMessagesUseCase
import domain.support.usecase.RetrySupportMessageUseCase
import domain.support.usecase.SendSupportMessageUseCase
import fakes.FakeSupportRepository
import feature.support.model.SupportTimelineItem
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SupportViewModelTest : ViewModelTestBase() {

    private val repository = FakeSupportRepository()

    private fun createViewModel() = SupportViewModel(
        observeMessages = ObserveSupportMessagesUseCase(repository),
        refreshMessages = RefreshSupportMessagesUseCase(repository),
        sendMessage = SendSupportMessageUseCase(repository),
        retryMessage = RetrySupportMessageUseCase(repository),
        markRead = MarkSupportReadUseCase(repository),
        // One tick: an endless poll would never let runTest go idle
        pollTicks = flowOf(Unit),
    )

    private fun adminMessage(id: String, body: String) =
        SupportMessage(id, SupportSender.ADMIN, body, createdAtMillis = 0, status = SupportDeliveryStatus.SENT)

    @Test
    fun `init loads the local conversation and refreshes once`() = runTest {
        repository.messages.value = listOf(adminMessage("s-1", "Welcome"))

        val vm = createViewModel()

        assertFalse(vm.currentState.isLoading)
        assertEquals(listOf("Welcome"), vm.currentState.messages.map { it.body })
        assertEquals(1, repository.refreshCount)
        assertFalse(vm.currentState.isOffline)
    }

    @Test
    fun `init builds the timeline with a day header before the messages`() = runTest {
        repository.messages.value = listOf(adminMessage("s-1", "Welcome"), adminMessage("s-2", "Hi"))

        val vm = createViewModel()

        val timeline = vm.currentState.timeline
        assertIs<SupportTimelineItem.DayHeader>(timeline.first())
        assertEquals(listOf("s-1", "s-2"), timeline.filterIsInstance<SupportTimelineItem.Bubble>().map { it.message.id })
    }

    @Test
    fun `remaining characters show only near the limit`() = runTest {
        val vm = createViewModel()

        vm.onDraftChanged("short")
        assertNull(vm.currentState.remainingChars)

        vm.onDraftChanged("x".repeat(MAX_SUPPORT_MESSAGE_LENGTH - 50))
        assertEquals(50, vm.currentState.remainingChars)
    }

    @Test
    fun `opening the chat marks replies as read`() = runTest {
        repository.messages.value = listOf(adminMessage("s-1", "Welcome"))
        repository.unread.value = SupportUnread(inChat = true, onSettings = true)

        createViewModel()

        assertEquals(SupportUnread(), repository.unread.value)
        assertTrue(repository.markReadCount >= 1)
    }

    @Test
    fun `failed refresh shows offline but keeps messages`() = runTest {
        repository.messages.value = listOf(adminMessage("s-1", "Welcome"))
        repository.refreshResult = Try.failure(IllegalStateException("offline"))

        val vm = createViewModel()

        assertTrue(vm.currentState.isOffline)
        assertEquals(1, vm.currentState.messages.size)
    }

    @Test
    fun `send clears the draft and the message appears`() = runTest {
        val vm = createViewModel()
        vm.onDraftChanged("  Sync is broken  ")

        vm.send()

        assertEquals("", vm.currentState.draft)
        assertEquals(listOf("Sync is broken"), repository.sent)
        assertEquals(listOf("Sync is broken"), vm.currentState.messages.map { it.body })
    }

    @Test
    fun `blank draft is not sent`() = runTest {
        val vm = createViewModel()
        vm.onDraftChanged("   ")

        vm.send()

        assertFalse(vm.currentState.canSend)
        assertTrue(repository.sent.isEmpty())
    }

    @Test
    fun `draft is restored when the message could not be stored`() = runTest {
        repository.sendResult = Try.failure(IllegalStateException("disk full"))
        val vm = createViewModel()
        vm.onDraftChanged("keep me")

        vm.send()

        assertEquals("keep me", vm.currentState.draft)
    }

    @Test
    fun `draft is capped at the backend limit`() = runTest {
        val vm = createViewModel()

        vm.onDraftChanged("x".repeat(MAX_SUPPORT_MESSAGE_LENGTH + 10))

        assertEquals(MAX_SUPPORT_MESSAGE_LENGTH, vm.currentState.draft.length)
    }

    @Test
    fun `retry passes the message id through`() = runTest {
        val vm = createViewModel()

        vm.retry("c-9")

        assertEquals(listOf("c-9"), repository.retried)
    }
}
