package feature.support

import domain.support.model.SupportUnread
import domain.support.usecase.MarkSupportSeenInSettingsUseCase
import domain.support.usecase.ObserveSupportUnreadUseCase
import fakes.FakeSupportRepository
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class SupportBadgeViewModelTest : ViewModelTestBase() {

    private val repository = FakeSupportRepository()

    private fun createViewModel() = SupportBadgeViewModel(
        observeUnread = ObserveSupportUnreadUseCase(repository),
        markSeenInSettings = MarkSupportSeenInSettingsUseCase(repository),
    )

    @Test
    fun `state follows the unread replies`() = runTest {
        val vm = createViewModel()
        assertEquals(SupportUnread(), vm.currentState)

        repository.unread.value = SupportUnread(inChat = true, onSettings = true)

        assertEquals(SupportUnread(inChat = true, onSettings = true), vm.currentState)
    }

    @Test
    fun `onSettingsOpened clears the settings dot but keeps the chat unread`() = runTest {
        repository.unread.value = SupportUnread(inChat = true, onSettings = true)
        val vm = createViewModel()

        vm.onSettingsOpened()

        assertEquals(SupportUnread(inChat = true, onSettings = false), vm.currentState)
        assertEquals(1, repository.markSeenInSettingsCount)
    }
}
