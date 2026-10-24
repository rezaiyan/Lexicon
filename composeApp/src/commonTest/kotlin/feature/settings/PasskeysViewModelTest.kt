package feature.settings

import core.error.DomainError
import domain.passkey.model.Passkey
import domain.passkey.usecase.CreatePasskeyUseCase
import domain.passkey.usecase.DeletePasskeyUseCase
import domain.passkey.usecase.GetPasskeysUseCase
import domain.passkey.usecase.IsPasskeyAvailableUseCase
import fakes.FakePasskeyAuthenticator
import fakes.FakePasskeyRepository
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PasskeysViewModelTest : ViewModelTestBase() {

    private val phone = Passkey(id = 1, createdAtMillis = 1_000, lastUsedAtMillis = 2_000, synced = true)
    private val repository = FakePasskeyRepository(passkeys = listOf(phone))
    private val device = FakePasskeyAuthenticator()

    private fun createViewModel() = PasskeysViewModel(
        isPasskeyAvailable = IsPasskeyAvailableUseCase(device),
        getPasskeys = GetPasskeysUseCase(repository),
        createPasskey = CreatePasskeyUseCase(repository, device),
        deletePasskey = DeletePasskeyUseCase(repository),
    )

    @Test
    fun `init reports whether the device can create passkeys`() = runTest {
        assertTrue(createViewModel().currentState.available)

        device.available = false
        assertFalse(createViewModel().currentState.available)
    }

    @Test
    fun `refresh loads the account's passkeys`() = runTest {
        val vm = createViewModel()

        vm.refresh()

        assertEquals(listOf(phone), vm.currentState.passkeys)
    }

    @Test
    fun `create registers what the device returns for the server's options`() = runTest {
        val vm = createViewModel()
        vm.refresh()

        vm.create()

        assertEquals(listOf("created:creation-options"), repository.registeredResponses)
        assertEquals(2, vm.currentState.passkeys.size)
        assertFalse(vm.currentState.creating)
        assertNull(vm.currentState.error)
    }

    @Test
    fun `create cancelled on the device registers nothing and shows no error`() = runTest {
        val vm = createViewModel()
        device.nextFailure = DomainError.Passkey.Cancelled

        vm.create()

        assertTrue(repository.registeredResponses.isEmpty())
        assertFalse(vm.currentState.creating)
        assertNull(vm.currentState.error)
    }

    @Test
    fun `create when the device already has a passkey explains why`() = runTest {
        val vm = createViewModel()
        device.nextFailure = DomainError.Passkey.AlreadyRegistered

        vm.create()

        assertNotNull(vm.currentState.error)
        assertFalse(vm.currentState.creating)
    }

    @Test
    fun `delete removes the passkey once the server confirms`() = runTest {
        val vm = createViewModel()
        vm.refresh()

        vm.delete(1)

        assertEquals(listOf(1L), repository.deleted)
        assertTrue(vm.currentState.passkeys.isEmpty())
        assertNull(vm.currentState.deletingId)
    }

    @Test
    fun `failed delete keeps the passkey and shows an error`() = runTest {
        val vm = createViewModel()
        vm.refresh()
        repository.failure = DomainError.Network.NoConnection

        vm.delete(1)

        assertEquals(listOf(phone), vm.currentState.passkeys)
        assertNull(vm.currentState.deletingId)
        assertNotNull(vm.currentState.error)
    }
}
