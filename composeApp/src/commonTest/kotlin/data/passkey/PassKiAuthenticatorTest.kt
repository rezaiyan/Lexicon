package data.passkey

import com.alirezaiyan.passki.PasskeyError
import com.alirezaiyan.passki.test.FakePasskeys
import com.alirezaiyan.passki.test.TestOptions
import core.common.Try
import core.error.DomainError
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
    fun `closing the sheet is Cancelled, not an error to show`() = runTest {
        device.failNextWith(PasskeyError.Cancelled)

        val result = authenticator.create(TestOptions.creation(rpId))

        assertEquals(DomainError.Passkey.Cancelled, (result as Try.Failure).throwable)
    }
}
