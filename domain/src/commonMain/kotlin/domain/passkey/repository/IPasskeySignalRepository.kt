package domain.passkey.repository

import core.common.Try
import domain.passkey.model.PasskeySignalsSent

/**
 * Local-only memory of what this device last told its password manager about the account's passkeys.
 * Password managers announce every signal ("passkey updated"), so signals go out only when this differs.
 */
interface IPasskeySignalRepository {
    /** Null when nothing was sent yet (or since sign-out). */
    suspend fun lastSent(): Try<PasskeySignalsSent?>
    suspend fun save(sent: PasskeySignalsSent): Try<Unit>
}
