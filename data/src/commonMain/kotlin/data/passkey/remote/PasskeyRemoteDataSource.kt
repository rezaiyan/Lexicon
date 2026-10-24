package data.passkey.remote

import core.common.Try
import core.common.doOnFailure
import core.common.map
import data.auth.remote.model.PasskeyResponseRequest
import data.core.network.client.ApiClient
import expects.logNetwork
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PasskeyResponse(
    val id: Long,
    /** Epoch millis. */
    val createdAt: Long,
    /** Epoch millis. */
    val lastUsedAt: Long? = null,
    val synced: Boolean = false,
)

/** Passkey management while signed in, and sign-in options (the sign-in itself is in `AuthDataSource`). */
interface IPasskeyRemoteDataSource {
    /** WebAuthn creation options JSON. */
    suspend fun registrationOptions(): Try<String>
    suspend fun register(responseJson: String): Try<PasskeyResponse>
    suspend fun getPasskeys(): Try<List<PasskeyResponse>>
    suspend fun delete(id: Long): Try<Unit>

    /** WebAuthn request options JSON. */
    suspend fun signInOptions(): Try<String>
}

class PasskeyRemoteDataSource(
    private val apiClient: ApiClient,
) : IPasskeyRemoteDataSource {

    // Options go to the device as JSON text: kept as a tree, never mapped to a model
    override suspend fun registrationOptions(): Try<String> =
        apiClient.postNotNull<JsonObject>("$PATH/registration-options")
            .map { it.toString() }
            .doOnFailure { logNetwork(TAG, "Registration options failed: ${it.message}") }

    override suspend fun register(responseJson: String): Try<PasskeyResponse> =
        apiClient.postNotNull<PasskeyResponse>(PATH, PasskeyResponseRequest(responseJson))
            .doOnFailure { logNetwork(TAG, "Register failed: ${it.message}") }

    override suspend fun getPasskeys(): Try<List<PasskeyResponse>> =
        apiClient.getNotNull<List<PasskeyResponse>>(PATH)
            .doOnFailure { logNetwork(TAG, "Fetch failed: ${it.message}") }

    override suspend fun delete(id: Long): Try<Unit> =
        apiClient.delete("$PATH/$id")
            .doOnFailure { logNetwork(TAG, "Delete failed: ${it.message}") }

    override suspend fun signInOptions(): Try<String> =
        apiClient.postNotNull<JsonObject>("$PATH/sign-in-options")
            .map { it.toString() }
            .doOnFailure { logNetwork(TAG, "Sign-in options failed: ${it.message}") }

    private companion object {
        const val PATH = "/auth/passkeys"
        const val TAG = "PasskeyRemote"
    }
}
