package domain.passkey

import core.common.Try
import domain.passkey.model.PasskeyAccountName
import domain.passkey.model.PasskeySignIn

/**
 * The device's passkey sheet. Takes the server's WebAuthn options JSON and returns the response
 * JSON for the server to verify; it never talks to the server itself.
 *
 * Failures are [core.error.DomainError.Passkey] (Cancelled when the user closes the sheet).
 */
interface IPasskeyAuthenticator {
    /** False where passkeys can't work (web, old OS versions): hide passkey actions there. */
    suspend fun isAvailable(): Boolean

    suspend fun create(optionsJson: String): Try<String>

    suspend fun signIn(optionsJson: String): Try<PasskeySignIn>

    /**
     * Tells the password manager the server no longer knows [credentialId], so it stops offering it.
     * Best effort: older iOS versions and some Android password managers can't, and fail as Unavailable.
     */
    suspend fun forget(rpId: String, credentialId: String): Try<Unit>

    /**
     * Tells the password manager that [credentialIds] are all of [userHandle]'s passkeys, so it drops
     * the ones deleted elsewhere. Best effort, like [forget].
     */
    suspend fun keepOnly(rpId: String, userHandle: String, credentialIds: List<String>): Try<Unit>

    /** Updates the account label on [userHandle]'s passkeys in the password manager. Best effort, like [forget]. */
    suspend fun relabel(rpId: String, userHandle: String, accountName: PasskeyAccountName): Try<Unit>
}
