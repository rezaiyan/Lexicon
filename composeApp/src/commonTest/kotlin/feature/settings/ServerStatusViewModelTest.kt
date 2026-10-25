package feature.settings

import domain.connectivity.model.ServerStatus
import domain.connectivity.usecase.CheckServerStatusUseCase
import domain.connectivity.usecase.ObserveServerStatusUseCase
import fakes.FakeServerStatusRepository
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ServerStatusViewModelTest : ViewModelTestBase() {

    private val repository = FakeServerStatusRepository(ServerStatus.Online)

    private fun createViewModel() = ServerStatusViewModel(
        observeServerStatus = ObserveServerStatusUseCase(repository),
        checkServerStatus = CheckServerStatusUseCase(repository),
    )

    @Test
    fun `state follows the live status as the server goes down and comes back`() = runTest {
        val vm = createViewModel()
        assertEquals(ServerStatus.Online, vm.currentState.status)

        repository.status.value = ServerStatus.Unavailable
        assertEquals(ServerStatus.Unavailable, vm.currentState.status)

        repository.status.value = ServerStatus.Online
        assertEquals(ServerStatus.Online, vm.currentState.status)
    }

    @Test
    fun `retry checks the server and shows what it found`() = runTest {
        repository.status.value = ServerStatus.Unavailable
        repository.nextCheck = ServerStatus.Online
        val vm = createViewModel()

        vm.retry()

        assertEquals(ServerStatus.Online, vm.currentState.status)
        assertFalse(vm.currentState.retrying)
        assertEquals(1, repository.checks)
    }

    @Test
    fun `retry while still down keeps unavailable`() = runTest {
        repository.status.value = ServerStatus.Unavailable
        repository.nextCheck = ServerStatus.Unavailable
        val vm = createViewModel()

        vm.retry()

        assertEquals(ServerStatus.Unavailable, vm.currentState.status)
        assertFalse(vm.currentState.retrying)
    }
}
