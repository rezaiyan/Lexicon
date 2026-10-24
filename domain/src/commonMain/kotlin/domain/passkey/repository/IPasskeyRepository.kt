package domain.passkey.repository

import core.common.Try
import domain.passkey.model.Passkey

/**
 * Passkeys live on the server only (it verifies every sign-in), so every call goes to the network.
 * Options are WebAuthn JSON, passed to the device unchanged.
 */
interface IPasskeyRepository {
    /** Creation options for the signed-in user. */
    suspend fun registrationOptions(): Try<String>

    /** Stores the passkey the device created from [registrationOptions]. */
    suspend fun register(responseJson: String): Try<Passkey>

    suspend fun getPasskeys(): Try<List<Passkey>>

    suspend fun delete(id: Long): Try<Unit>

    /** Sign-in options; needs no session. */
    suspend fun signInOptions(): Try<String>
}
