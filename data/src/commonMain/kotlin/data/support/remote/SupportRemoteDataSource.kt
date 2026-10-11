package data.support.remote

import core.common.Try
import core.common.doOnFailure
import core.common.map
import data.core.network.client.ApiClient
import expects.logNetwork
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

@Serializable
data class SendSupportMessageRequest(
    val body: String,
    val clientMessageId: String,
)

@Serializable
data class SupportMessageResponse(
    val id: Long,
    /** USER or ADMIN. */
    val sender: String,
    val body: String,
    val clientMessageId: String? = null,
    /** Epoch millis. */
    val createdAt: Long,
)

@Serializable
data class SupportMessagesResponse(
    val messages: List<SupportMessageResponse> = emptyList(),
    val unreadCount: Long = 0,
)

interface ISupportRemoteDataSource {
    /** Messages with a server id greater than [afterId], oldest first. */
    suspend fun getMessages(afterId: Long): Try<List<SupportMessageResponse>>

    /** Idempotent on [SendSupportMessageRequest.clientMessageId]: a retry returns the stored message. */
    suspend fun send(request: SendSupportMessageRequest): Try<SupportMessageResponse>
}

class SupportRemoteDataSource(
    private val apiClient: ApiClient,
) : ISupportRemoteDataSource {

    override suspend fun getMessages(afterId: Long): Try<List<SupportMessageResponse>> =
        apiClient.getNotNull<SupportMessagesResponse>("/support/messages") { parameter("after", afterId) }
            .map { it.messages }
            .doOnFailure { logNetwork("SupportRemote", "Fetch failed: ${it.message}") }

    override suspend fun send(request: SendSupportMessageRequest): Try<SupportMessageResponse> =
        apiClient.postNotNull<SupportMessageResponse>("/support/messages", body = request)
            .doOnFailure { logNetwork("SupportRemote", "Send failed: ${it.message}") }
}
