package data.passkey

import com.alirezaiyan.passki.PasskeyError
import com.alirezaiyan.passki.PasskeyResult
import com.alirezaiyan.passki.Passkeys
import com.alirezaiyan.passki.SignInMode
import core.common.Try
import core.error.DomainError
import domain.passkey.IPasskeyAuthenticator
import domain.passkey.model.PasskeyAccountName
import domain.passkey.model.PasskeySignIn
import expects.logPlatform
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
    override suspend fun signIn(optionsJson: String): Try<PasskeySignIn> {
        val mode = if (passkeys.capabilities().immediateSignIn) SignInMode.ImmediateOnly else SignInMode.Modal
        return passkeys.signIn(optionsJson, mode).toTry("sign-in") {
            PasskeySignIn(responseJson = it.json, credentialId = it.credentialId, rpId = rpIdOf(optionsJson))
        }
    }

    override suspend fun forget(rpId: String, credentialId: String): Try<Unit> =
        passkeys.signals.unknownCredential(rpId, credentialId).toTry("forget") { }

    override suspend fun keepOnly(rpId: String, userHandle: String, credentialIds: List<String>): Try<Unit> =
        passkeys.signals.acceptedCredentials(rpId, userHandle, credentialIds).toTry("keep-only") { }

    override suspend fun relabel(rpId: String, userHandle: String, accountName: PasskeyAccountName): Try<Unit> =
        passkeys.signals.currentUserDetails(rpId, userHandle, accountName.name, accountName.displayName)
            .toTry("relabel") { }

    private inline fun <T, R> PasskeyResult<T>.toTry(ceremony: String, transform: (T) -> R): Try<R> = when (this) {
        is PasskeyResult.Success -> Try.success(transform(value))
        is PasskeyResult.Failure -> Try.failure(error.toDomain(ceremony))
    }

    /** PassKi already refused options without an RP ID, so a successful sign-in always has one. */
    private fun rpIdOf(optionsJson: String): String {
        val options = Json.parseToJsonElement(optionsJson).jsonObject
        val publicKey = options["publicKey"]?.jsonObject ?: options
        return publicKey["rpId"]?.jsonPrimitive?.content.orEmpty()
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
            is PasskeyError.NotSupported -> DomainError.Passkey.Unavailable
            // Our domain or app setup, not the user's device: the log line above names the check
            is PasskeyError.Misconfigured -> DomainError.Passkey.SetupProblem
            is PasskeyError.InvalidOptions, is PasskeyError.Unknown -> DomainError.Passkey.Failed(toString())
        }
    }

    private companion object {
        const val TAG = "Passkey"
    }
}
