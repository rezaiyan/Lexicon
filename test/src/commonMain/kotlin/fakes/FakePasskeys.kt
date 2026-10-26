package fakes

import core.common.Try
import core.common.map
import core.error.DomainError
import domain.passkey.IPasskeyAuthenticator
import domain.passkey.model.AccountPasskeys
import domain.passkey.model.Passkey
import domain.passkey.model.PasskeyAccountName
import domain.passkey.model.PasskeySignIn
import domain.passkey.model.PasskeySignalsSent
import domain.passkey.repository.IPasskeyRepository
import domain.passkey.repository.IPasskeySignalRepository

const val FAKE_RP_ID = "lexicon.alirezaiyan.com"
val FAKE_ACCOUNT_NAME = PasskeyAccountName(name = "pat@gmail.com", displayName = "Pat")

/** The device passkey sheet: echoes the options back as the response unless [nextFailure] is set. */
class FakePasskeyAuthenticator(
    var available: Boolean = true,
) : IPasskeyAuthenticator {
    /** Fails the next create / sign-in once with it. */
    var nextFailure: Throwable? = null
    val requests = mutableListOf<String>()

    /** The passkey the user picks in the sign-in sheet. */
    var signInCredentialId = "credential-1"

    /** False: the password manager ignores signals (older iOS, some Android providers). */
    var signalsSupported = true

    /** Credential ids the password manager was told to forget. */
    val forgotten = mutableListOf<String>()

    /** The last set of credential ids the password manager was told to keep. */
    var keptOnly: List<String>? = null

    /** The last account label the password manager was told to show. */
    var accountName: PasskeyAccountName? = null

    /** Every keep-only / relabel signal sent, including ones the password manager ignored. */
    var keepOnlyCalls = 0
    var relabelCalls = 0

    override suspend fun isAvailable(): Boolean = available

    override suspend fun create(optionsJson: String): Try<String> = respond("created:$optionsJson")

    override suspend fun signIn(optionsJson: String): Try<PasskeySignIn> =
        respond("signed:$optionsJson").map { PasskeySignIn(it, signInCredentialId, FAKE_RP_ID) }

    override suspend fun forget(rpId: String, credentialId: String): Try<Unit> = signal { forgotten += credentialId }

    override suspend fun keepOnly(rpId: String, userHandle: String, credentialIds: List<String>): Try<Unit> {
        keepOnlyCalls++
        return signal { keptOnly = credentialIds }
    }

    override suspend fun relabel(rpId: String, userHandle: String, accountName: PasskeyAccountName): Try<Unit> {
        relabelCalls++
        return signal { this.accountName = accountName }
    }

    private fun respond(response: String): Try<String> {
        nextFailure?.let { nextFailure = null; return Try.failure(it) }
        requests += response
        return Try.success(response)
    }

    private inline fun signal(record: () -> Unit): Try<Unit> =
        if (signalsSupported) Try.success(record()) else Try.failure(DomainError.Passkey.Unavailable)
}

class FakePasskeyRepository(
    var passkeys: List<Passkey> = emptyList(),
) : IPasskeyRepository {
    /** When set, every call fails with it. */
    var failure: Throwable? = null
    var accountName: PasskeyAccountName? = FAKE_ACCOUNT_NAME
    var userHandle = "user-handle"
    val registeredResponses = mutableListOf<String>()
    val deleted = mutableListOf<Long>()

    override suspend fun registrationOptions(): Try<String> = result { "creation-options" }

    override suspend fun register(responseJson: String): Try<Passkey> = result {
        registeredResponses += responseJson
        val id = (passkeys.maxOfOrNull { it.id } ?: 0) + 1
        fakePasskey(id).also { passkeys = passkeys + it }
    }

    override suspend fun getPasskeys(): Try<AccountPasskeys> = result {
        AccountPasskeys(FAKE_RP_ID, userHandle = userHandle.takeIf { passkeys.isNotEmpty() }, accountName, passkeys)
    }

    override suspend fun delete(id: Long): Try<Unit> = result {
        deleted += id
        passkeys = passkeys.filterNot { it.id == id }
    }

    override suspend fun signInOptions(): Try<String> = result { "request-options" }

    private inline fun <T> result(value: () -> T): Try<T> = failure?.let { Try.failure(it) } ?: Try.success(value())
}

/** What this device last told its password manager, kept in memory. */
class FakePasskeySignalRepository(
    var sent: PasskeySignalsSent? = null,
) : IPasskeySignalRepository {
    override suspend fun lastSent(): Try<PasskeySignalsSent?> = Try.success(sent)

    override suspend fun save(sent: PasskeySignalsSent): Try<Unit> = Try.success(Unit).also { this.sent = sent }
}

fun fakePasskey(id: Long, lastUsedAtMillis: Long? = null) = Passkey(
    id = id,
    credentialId = "credential-$id",
    createdAtMillis = 0,
    lastUsedAtMillis = lastUsedAtMillis,
    synced = true,
)
