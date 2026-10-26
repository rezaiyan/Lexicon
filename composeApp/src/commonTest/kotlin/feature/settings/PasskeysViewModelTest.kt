package feature.settings

import core.error.DomainError
import domain.passkey.usecase.CreatePasskeyUseCase
import domain.passkey.usecase.DeletePasskeyUseCase
import domain.passkey.usecase.GetPasskeysUseCase
import domain.passkey.model.PasskeyAccountName
import domain.passkey.usecase.IsPasskeyAvailableUseCase
import fakes.FAKE_ACCOUNT_NAME
import fakes.FakePasskeyAuthenticator
import fakes.FakePasskeyRepository
import fakes.FakePasskeySignalRepository
import fakes.fakePasskey
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PasskeysViewModelTest : ViewModelTestBase() {

    private val phone = fakePasskey(id = 1, lastUsedAtMillis = 2_000)
    private val repository = FakePasskeyRepository(passkeys = listOf(phone))
    private val device = FakePasskeyAuthenticator()
    private val signalsSent = FakePasskeySignalRepository()

    private fun createViewModel() = PasskeysViewModel(
        isPasskeyAvailable = IsPasskeyAvailableUseCase(device),
        getPasskeys = GetPasskeysUseCase(repository, device, signalsSent),
        createPasskey = CreatePasskeyUseCase(repository, device),
        deletePasskey = DeletePasskeyUseCase(repository, device),
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
    fun `refresh tells the device which passkeys still exist so deleted ones leave its sign-in sheet`() = runTest {
        val vm = createViewModel()

        vm.refresh()

        assertEquals(listOf(phone.credentialId), device.keptOnly)
    }

    @Test
    fun `refresh labels this device's passkeys with their account so several accounts are told apart`() = runTest {
        val vm = createViewModel()

        vm.refresh()

        assertEquals(FAKE_ACCOUNT_NAME, device.accountName)
    }

    @Test
    fun `refresh leaves the labels alone when the server doesn't send one`() = runTest {
        repository.accountName = null
        val vm = createViewModel()

        vm.refresh()

        assertNull(device.accountName)
        assertEquals(listOf(phone), vm.currentState.passkeys)
    }

    @Test
    fun `refresh again with nothing changed sends the password manager nothing`() = runTest {
        createViewModel().refresh()

        createViewModel().refresh()

        assertEquals(1, device.keepOnlyCalls)
        assertEquals(1, device.relabelCalls)
    }

    @Test
    fun `refresh after a passkey changed elsewhere resends only the list`() = runTest {
        createViewModel().refresh()
        repository.passkeys = listOf(phone, fakePasskey(id = 2))

        createViewModel().refresh()

        assertEquals(listOf(phone.credentialId, "credential-2"), device.keptOnly)
        assertEquals(2, device.keepOnlyCalls)
        assertEquals(1, device.relabelCalls)
    }

    @Test
    fun `refresh after the account name changed resends only the label`() = runTest {
        createViewModel().refresh()
        val renamed = PasskeyAccountName(name = "pat@work.com", displayName = "Pat")
        repository.accountName = renamed

        createViewModel().refresh()

        assertEquals(renamed, device.accountName)
        assertEquals(1, device.keepOnlyCalls)
        assertEquals(2, device.relabelCalls)
    }

    @Test
    fun `refresh for another account on this device sends both signals`() = runTest {
        createViewModel().refresh()
        repository.userHandle = "other-user"

        createViewModel().refresh()

        assertEquals(2, device.keepOnlyCalls)
        assertEquals(2, device.relabelCalls)
    }

    @Test
    fun `refresh retries signals the password manager did not take`() = runTest {
        device.signalsSupported = false
        createViewModel().refresh()
        device.signalsSupported = true

        createViewModel().refresh()

        assertEquals(listOf(phone.credentialId), device.keptOnly)
        assertEquals(FAKE_ACCOUNT_NAME, device.accountName)
        assertEquals(2, device.keepOnlyCalls)
        assertEquals(2, device.relabelCalls)
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
    fun `delete asks the device to forget the passkey`() = runTest {
        val vm = createViewModel()
        vm.refresh()

        vm.delete(1)

        assertEquals(listOf(phone.credentialId), device.forgotten)
    }

    @Test
    fun `delete still succeeds where the device can't forget passkeys`() = runTest {
        val vm = createViewModel()
        vm.refresh()
        device.signalsSupported = false

        vm.delete(1)

        assertEquals(listOf(1L), repository.deleted)
        assertTrue(vm.currentState.passkeys.isEmpty())
        assertNull(vm.currentState.error)
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
        assertTrue(device.forgotten.isEmpty())
    }
}
