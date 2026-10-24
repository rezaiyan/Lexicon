package fakes

import core.common.Try
import domain.passkey.IPasskeyAuthenticator
import domain.passkey.model.Passkey
import domain.passkey.repository.IPasskeyRepository

/** The device passkey sheet: echoes the options back as the response unless [nextFailure] is set. */
class FakePasskeyAuthenticator(
    var available: Boolean = true,
) : IPasskeyAuthenticator {
    /** Fails the next create / sign-in once with it. */
    var nextFailure: Throwable? = null
    val requests = mutableListOf<String>()

    override suspend fun isAvailable(): Boolean = available

    override suspend fun create(optionsJson: String): Try<String> = respond("created:$optionsJson")

    override suspend fun signIn(optionsJson: String): Try<String> = respond("signed:$optionsJson")

    private fun respond(response: String): Try<String> {
        nextFailure?.let { nextFailure = null; return Try.failure(it) }
        requests += response
        return Try.success(response)
    }
}

class FakePasskeyRepository(
    var passkeys: List<Passkey> = emptyList(),
) : IPasskeyRepository {
    /** When set, every call fails with it. */
    var failure: Throwable? = null
    val registeredResponses = mutableListOf<String>()
    val deleted = mutableListOf<Long>()

    override suspend fun registrationOptions(): Try<String> = result { "creation-options" }

    override suspend fun register(responseJson: String): Try<Passkey> = result {
        registeredResponses += responseJson
        Passkey(id = (passkeys.maxOfOrNull { it.id } ?: 0) + 1, createdAtMillis = 0, lastUsedAtMillis = null, synced = true)
            .also { passkeys = passkeys + it }
    }

    override suspend fun getPasskeys(): Try<List<Passkey>> = result { passkeys }

    override suspend fun delete(id: Long): Try<Unit> = result {
        deleted += id
        passkeys = passkeys.filterNot { it.id == id }
    }

    override suspend fun signInOptions(): Try<String> = result { "request-options" }

    private inline fun <T> result(value: () -> T): Try<T> = failure?.let { Try.failure(it) } ?: Try.success(value())
}
