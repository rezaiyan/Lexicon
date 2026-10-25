package data.passkey

import com.alirezaiyan.passki.PasskeyError
import com.alirezaiyan.passki.test.FakePasskeys
import com.alirezaiyan.passki.test.FakeSignal
import com.alirezaiyan.passki.test.TestOptions
import core.common.Try
import core.common.getOrThrow
import core.error.DomainError
import domain.passkey.model.PasskeyAccountName
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PassKiAuthenticatorTest {

    private val rpId = "lexicon.alirezaiyan.com"
    private val device = FakePasskeys(origin = "https://$rpId")
    private val authenticator = PassKiAuthenticator(device)

    @Test
    fun `sign-in on a device without a passkey fails fast as NoPasskeys`() = runTest {
        val result = authenticator.signIn(TestOptions.request(rpId))

        assertEquals(DomainError.Passkey.NoPasskeys, (result as Try.Failure).throwable)
    }

    @Test
    fun `sign-in after creating a passkey returns the device response`() = runTest {
        assertTrue(authenticator.create(TestOptions.creation(rpId)).isSuccess)

        val result = authenticator.signIn(TestOptions.request(rpId))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `sign-in falls back to the full sheet where device-only sign-in is missing`() = runTest {
        authenticator.create(TestOptions.creation(rpId))
        device.immediateSignIn = false

        val result = authenticator.signIn(TestOptions.request(rpId))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `sign-in reports which passkey was used and for which domain`() = runTest {
        authenticator.create(TestOptions.creation(rpId))

        val signIn = authenticator.signIn(TestOptions.request(rpId)).getOrThrow()

        assertEquals(rpId, signIn.rpId)
        assertTrue(signIn.credentialId.isNotBlank())
    }

    @Test
    fun `a forgotten passkey is no longer offered at sign-in`() = runTest {
        authenticator.create(TestOptions.creation(rpId))
        val signIn = authenticator.signIn(TestOptions.request(rpId)).getOrThrow()

        assertTrue(authenticator.forget(rpId, signIn.credentialId).isSuccess)

        val again = authenticator.signIn(TestOptions.request(rpId))
        assertEquals(DomainError.Passkey.NoPasskeys, (again as Try.Failure).throwable)
    }

    @Test
    fun `relabel tells the password manager which account the passkeys are for`() = runTest {
        val result = authenticator.relabel(rpId, "dXNlcg", PasskeyAccountName("Ali (Apple ID)", "Ali"))

        assertTrue(result.isSuccess)
        assertEquals(
            FakeSignal.CurrentUserDetails(rpId, "dXNlcg", "Ali (Apple ID)", "Ali"),
            device.receivedSignals.single(),
        )
    }

    @Test
    fun `where the password manager ignores signals forgetting fails as Unavailable`() = runTest {
        device.signalsSupported = false

        val result = authenticator.forget(rpId, "Y3JlZA")

        assertEquals(DomainError.Passkey.Unavailable, (result as Try.Failure).throwable)
    }

    @Test
    fun `a setup problem on our side is not blamed on the device`() = runTest {
        device.failNextWith(PasskeyError.Misconfigured("RP ID cannot be validated.", "Serve assetlinks.json", "AND-03"))

        val result = authenticator.create(TestOptions.creation(rpId))

        assertEquals(DomainError.Passkey.SetupProblem, (result as Try.Failure).throwable)
    }

    @Test
    fun `closing the sheet is Cancelled not an error to show`() = runTest {
        device.failNextWith(PasskeyError.Cancelled)

        val result = authenticator.create(TestOptions.creation(rpId))

        assertEquals(DomainError.Passkey.Cancelled, (result as Try.Failure).throwable)
    }
}
