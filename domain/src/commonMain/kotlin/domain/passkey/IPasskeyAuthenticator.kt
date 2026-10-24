package domain.passkey

import core.common.Try

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

    suspend fun signIn(optionsJson: String): Try<String>
}
