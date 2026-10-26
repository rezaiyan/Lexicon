package data.passkey

import com.alirezaiyan.passki.PassKi
import com.alirezaiyan.passki.PasskeyError
import com.alirezaiyan.passki.PasskeyResult
import com.alirezaiyan.passki.Passkeys
import com.alirezaiyan.passki.PasskiLogHandler
import com.alirezaiyan.passki.PasskiLogLevel
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
 * on iOS, navigator.credentials in the browser.
 */
class PassKiAuthenticator(
    // No Activity or window needed: PassKi finds the foreground screen itself
    private val passkeys: Passkeys = Passkeys(),
) : IPasskeyAuthenticator {

    init {
        // PassKi logs each failed call once: setup problems with their check id and fix, plus Warn for
        // NotSupported and Interrupted. Closing the sheet and having no passkey stay quiet.
        PassKi.logLevel = PasskiLogLevel.Warn
        PassKi.logHandler = PasskiLogHandler { _, tag, message, throwable ->
            logPlatform(tag, throwable?.let { "$message\n${it.stackTraceToString()}" } ?: message)
        }
    }

    override suspend fun isAvailable(): Boolean = passkeys.capabilities().isSupported

    override suspend fun create(optionsJson: String): Try<String> =
        passkeys.register(optionsJson).toTry { it.json }

    /**
     * Only passkeys already on this device: without one it fails at once as NoPasskeys instead of a
     * system sheet offering a QR code for another phone, which only confuses someone who never made one.
     */
    override suspend fun signIn(optionsJson: String): Try<PasskeySignIn> {
        val mode = if (passkeys.capabilities().immediateSignIn) SignInMode.ImmediateOnly else SignInMode.Modal
        var result = passkeys.signIn(optionsJson, mode)
        // A browser that claimed immediate sign-in but rejects it fails as NotSupported: use the full sheet
        val unsupported = (result as? PasskeyResult.Failure)?.error is PasskeyError.NotSupported
        if (mode == SignInMode.ImmediateOnly && unsupported) {
            result = passkeys.signIn(optionsJson, SignInMode.Modal)
        }
        return result.toTry {
            PasskeySignIn(responseJson = it.json, credentialId = it.credentialId, rpId = rpIdOf(optionsJson))
        }
    }

    override suspend fun forget(rpId: String, credentialId: String): Try<Unit> =
        passkeys.signals.unknownCredential(rpId, credentialId).toTry { }

    override suspend fun keepOnly(rpId: String, userHandle: String, credentialIds: List<String>): Try<Unit> =
        passkeys.signals.acceptedCredentials(rpId, userHandle, credentialIds).toTry { }

    override suspend fun relabel(rpId: String, userHandle: String, accountName: PasskeyAccountName): Try<Unit> =
        passkeys.signals.currentUserDetails(rpId, userHandle, accountName.name, accountName.displayName)
            .toTry { }

    private inline fun <T, R> PasskeyResult<T>.toTry(transform: (T) -> R): Try<R> = when (this) {
        is PasskeyResult.Success -> Try.success(transform(value))
        is PasskeyResult.Failure -> Try.failure(error.toDomain())
    }

    /** PassKi already refused options without an RP ID, so a successful sign-in always has one. */
    private fun rpIdOf(optionsJson: String): String {
        val options = Json.parseToJsonElement(optionsJson).jsonObject
        val publicKey = options["publicKey"]?.jsonObject ?: options
        return publicKey["rpId"]?.jsonPrimitive?.content.orEmpty()
    }

    private fun PasskeyError.toDomain(): DomainError.Passkey = when (this) {
        PasskeyError.Cancelled, PasskeyError.Interrupted -> DomainError.Passkey.Cancelled
        PasskeyError.NoCredentials -> DomainError.Passkey.NoPasskeys
        PasskeyError.AlreadyRegistered -> DomainError.Passkey.AlreadyRegistered
        is PasskeyError.NotSupported -> DomainError.Passkey.Unavailable
        // Our domain or app setup, not the user's device: PassKi's log line names the check and the fix
        is PasskeyError.Misconfigured -> DomainError.Passkey.SetupProblem
        is PasskeyError.InvalidOptions, is PasskeyError.Unknown -> DomainError.Passkey.Failed(toString())
    }
}
