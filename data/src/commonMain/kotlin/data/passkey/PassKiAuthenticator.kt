package data.passkey

import com.alirezaiyan.passki.PasskeyError
import com.alirezaiyan.passki.PasskeyResult
import com.alirezaiyan.passki.Passkeys
import com.alirezaiyan.passki.SignInMode
import core.common.Try
import core.error.DomainError
import domain.passkey.IPasskeyAuthenticator
import expects.logPlatform

/**
 * The system passkey sheet through PassKi: Credential Manager on Android, AuthenticationServices
 * on iOS. On the web every call fails as [DomainError.Passkey.Unavailable].
 */
class PassKiAuthenticator(
    // No Activity or window needed: PassKi finds the foreground screen itself
    private val passkeys: Passkeys = Passkeys(),
) : IPasskeyAuthenticator {

    override suspend fun isAvailable(): Boolean = passkeys.capabilities().isSupported

    override suspend fun create(optionsJson: String): Try<String> =
        passkeys.register(optionsJson).toTry("create") { it.json }

    /**
     * Only passkeys already on this device: without one it fails at once as NoPasskeys instead of a
     * system sheet offering a QR code for another phone, which only confuses someone who never made one.
     */
    override suspend fun signIn(optionsJson: String): Try<String> {
        val mode = if (passkeys.capabilities().immediateSignIn) SignInMode.ImmediateOnly else SignInMode.Modal
        return passkeys.signIn(optionsJson, mode).toTry("sign-in") { it.json }
    }

    private inline fun <T> PasskeyResult<T>.toTry(ceremony: String, json: (T) -> String): Try<String> = when (this) {
        is PasskeyResult.Success -> Try.success(json(value))
        is PasskeyResult.Failure -> Try.failure(error.toDomain(ceremony))
    }

    private fun PasskeyError.toDomain(ceremony: String): DomainError.Passkey {
        if (this !is PasskeyError.Cancelled) {
            // Misconfigured carries the fix and the PassKi check id (AND-03, IOS-02, …): the setup to repair
            val hint = (this as? PasskeyError.Misconfigured)?.let { " fix=${it.fixHint} check=${it.checkId}" }.orEmpty()
            logPlatform(TAG, "Passkey $ceremony failed: $this$hint")
        }
        return when (this) {
            PasskeyError.Cancelled, PasskeyError.Interrupted -> DomainError.Passkey.Cancelled
            PasskeyError.NoCredentials -> DomainError.Passkey.NoPasskeys
            PasskeyError.AlreadyRegistered -> DomainError.Passkey.AlreadyRegistered
            is PasskeyError.NotSupported, is PasskeyError.Misconfigured -> DomainError.Passkey.Unavailable
            is PasskeyError.InvalidOptions, is PasskeyError.Unknown -> DomainError.Passkey.Failed(toString())
        }
    }

    private companion object {
        const val TAG = "Passkey"
    }
}
