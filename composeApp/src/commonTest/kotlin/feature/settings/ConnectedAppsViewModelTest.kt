package feature.settings

import core.error.DomainError
import domain.connectedapps.model.ConnectedApp
import domain.connectedapps.model.ConnectedAppAccess
import domain.connectedapps.usecase.DisconnectAppUseCase
import domain.connectedapps.usecase.GetConnectedAppsUseCase
import fakes.FakeConnectedAppsRepository
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConnectedAppsViewModelTest : ViewModelTestBase() {

    private val claude = ConnectedApp("c1", "Claude", "claude.ai", setOf(ConnectedAppAccess.READ_WORDS), 1_000)
    private val chatGpt = ConnectedApp("c2", "ChatGPT", "chatgpt.com", ConnectedAppAccess.entries.toSet(), 2_000)
    private val repository = FakeConnectedAppsRepository(apps = listOf(claude, chatGpt))

    private fun createViewModel() = ConnectedAppsViewModel(
        getConnectedApps = GetConnectedAppsUseCase(repository),
        disconnectApp = DisconnectAppUseCase(repository),
    )

    @Test
    fun `init loads the connected apps`() = runTest {
        val vm = createViewModel()

        assertEquals(listOf(claude, chatGpt), vm.currentState.apps)
    }

    @Test
    fun `load failure leaves no apps so the settings row stays hidden`() = runTest {
        repository.failure = DomainError.Network.NoConnection

        val vm = createViewModel()

        assertEquals(emptyList(), vm.currentState.apps)
    }

    @Test
    fun `disconnect removes the app once the server confirms`() = runTest {
        val vm = createViewModel()

        vm.disconnect("c1")

        assertEquals(listOf("c1"), repository.disconnected)
        assertEquals(listOf(chatGpt), vm.currentState.apps)
        assertNull(vm.currentState.disconnectingId)
        assertNull(vm.currentState.failedId)
    }

    @Test
    fun `failed disconnect keeps the app and marks it failed`() = runTest {
        val vm = createViewModel()
        repository.failure = DomainError.Network.NoConnection

        vm.disconnect("c1")

        assertEquals(listOf(claude, chatGpt), vm.currentState.apps)
        assertEquals("c1", vm.currentState.failedId)
        assertNull(vm.currentState.disconnectingId)
    }

    @Test
    fun `refresh picks up apps connected since`() = runTest {
        val vm = createViewModel()
        val gemini = ConnectedApp("c3", "Gemini", null, emptySet(), 3_000)
        repository.apps = repository.apps + gemini

        vm.refresh()

        assertEquals(listOf(claude, chatGpt, gemini), vm.currentState.apps)
    }
}
