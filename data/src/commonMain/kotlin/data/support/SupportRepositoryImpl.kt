package data.support

import core.common.Try
import core.common.flatMap
import core.common.fold
import core.common.getOrDefault
import data.core.database.SupportMessageEntity
import data.support.local.ISupportLocalDataSource
import data.support.remote.ISupportRemoteDataSource
import data.support.remote.SendSupportMessageRequest
import data.support.remote.SupportMessageResponse
import domain.support.model.SupportDeliveryStatus
import domain.support.model.SupportMessage
import domain.support.model.SupportSender
import domain.support.repository.ISupportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random
import kotlin.time.Clock

/**
 * Local-first support chat: the UI reads SQLDelight only. A sent message is stored as SENDING,
 * then replaced by the server copy (same local id, now with a server id) or marked FAILED.
 */
class SupportRepositoryImpl(
    private val local: ISupportLocalDataSource,
    private val remote: ISupportRemoteDataSource,
    private val clock: Clock = Clock.System,
    private val random: Random = Random.Default,
) : ISupportRepository {

    override fun observeMessages(): Flow<List<SupportMessage>> =
        local.observe().map { rows -> rows.map { it.toDomain() } }

    override suspend fun refresh(): Try<Unit> {
        // Pull first: a send whose response was lost comes back with its client id and settles the row.
        val pulled = Try { local.maxServerId() }
            .flatMap { afterId -> remote.getMessages(afterId) }
            .flatMap { messages -> Try { local.upsert(messages.map { it.toEntity() }) } }
        // Sends are idempotent on the client id, so re-sending a row that is mid-flight is harmless.
        Try { local.unsent() }.getOrDefault(emptyList()).forEach { deliver(it) }
        return pulled
    }

    override suspend fun send(body: String): Try<Unit> {
        val message = SupportMessageEntity(
            localId = newClientId(),
            serverId = null,
            sender = SupportSender.USER.name,
            body = body,
            createdAt = clock.now().toEpochMilliseconds(),
            status = SupportDeliveryStatus.SENDING.name,
        )
        return Try { local.upsert(listOf(message)) }.also { stored ->
            if (stored is Try.Success) deliver(message)
        }
    }

    override suspend fun retry(messageId: String): Try<Unit> = Try {
        val message = local.find(messageId)
        if (message != null && message.serverId == null) {
            local.setStatus(messageId, SupportDeliveryStatus.SENDING.name)
            deliver(message)
        }
    }

    private suspend fun deliver(message: SupportMessageEntity) {
        remote.send(SendSupportMessageRequest(body = message.body, clientMessageId = message.localId)).fold(
            onSuccess = { sent -> Try { local.upsert(listOf(sent.toEntity())) } },
            onFailure = { Try { local.setStatus(message.localId, SupportDeliveryStatus.FAILED.name) } },
        )
    }

    private fun newClientId(): String =
        "${clock.now().toEpochMilliseconds()}-${random.nextLong().toULong().toString(CLIENT_ID_RADIX)}"

    private companion object {
        const val CLIENT_ID_RADIX = 36
    }
}

private fun SupportMessageResponse.toEntity() = SupportMessageEntity(
    localId = clientMessageId ?: "s-$id",
    serverId = id,
    sender = sender,
    body = body,
    createdAt = createdAt,
    status = SupportDeliveryStatus.SENT.name,
)

private fun SupportMessageEntity.toDomain() = SupportMessage(
    id = localId,
    sender = if (sender == SupportSender.ADMIN.name) SupportSender.ADMIN else SupportSender.USER,
    body = body,
    createdAtMillis = createdAt,
    status = SupportDeliveryStatus.entries.firstOrNull { it.name == status } ?: SupportDeliveryStatus.FAILED,
)
