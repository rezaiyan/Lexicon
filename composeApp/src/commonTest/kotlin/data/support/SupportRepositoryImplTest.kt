package data.support

import core.common.Try
import data.core.database.SupportMessageEntity
import core.common.getOrNull
import data.support.local.ISupportLocalDataSource
import data.support.local.ISupportReadStateLocalDataSource
import data.support.local.SupportUnreadRow
import data.support.remote.ISupportRemoteDataSource
import data.support.remote.SendSupportMessageRequest
import data.support.remote.SupportMessageResponse
import domain.support.model.SupportDeliveryStatus
import domain.support.model.SupportSender
import domain.support.model.SupportUnread
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** Read markers over [FakeSupportLocal]'s rows, computed like observeSupportUnread. */
private class FakeSupportReadState(private val local: FakeSupportLocal) : ISupportReadStateLocalDataSource {
    private val readServerId = MutableStateFlow(0L)
    private val settingsSeenServerId = MutableStateFlow(0L)
    private fun latestReplyId(values: Collection<SupportMessageEntity>) =
        values.filter { it.sender == "ADMIN" }.mapNotNull { it.serverId }.maxOrNull() ?: 0L

    override fun observeUnread(): Flow<SupportUnreadRow> =
        combine(local.rows, readServerId, settingsSeenServerId) { r, read, seen ->
            SupportUnreadRow(latestReplyId(r.values), read, seen)
        }

    override suspend fun markRead() {
        readServerId.value = latestReplyId(local.rows.value.values)
        settingsSeenServerId.value = readServerId.value
    }

    override suspend fun markSeenInSettings() {
        settingsSeenServerId.value = latestReplyId(local.rows.value.values)
    }
}

/** In-memory stand-in for the SQLDelight table, ordered like observeSupportMessages. */
private class FakeSupportLocal : ISupportLocalDataSource {
    val rows = MutableStateFlow<Map<String, SupportMessageEntity>>(emptyMap())

    private fun ordered(values: Collection<SupportMessageEntity> = rows.value.values) = values.sortedWith(
        compareBy<SupportMessageEntity> { it.serverId == null }.thenBy { it.serverId }.thenBy { it.createdAt }
    )

    override fun observe(): Flow<List<SupportMessageEntity>> = rows.map { ordered(it.values) }

    override suspend fun maxServerId(): Long = rows.value.values.mapNotNull { it.serverId }.maxOrNull() ?: 0L
    override suspend fun unsent(): List<SupportMessageEntity> = ordered().filter { it.serverId == null }
    override suspend fun find(localId: String): SupportMessageEntity? = rows.value[localId]
    override suspend fun upsert(messages: List<SupportMessageEntity>) {
        rows.value = rows.value + messages.associateBy { it.localId }
    }
    override suspend fun setStatus(localId: String, status: String) {
        val row = rows.value[localId] ?: return
        rows.value = rows.value + (localId to row.copy(status = status))
    }

    override suspend fun count(): Long = rows.value.size.toLong()

}

private class FakeSupportRemote : ISupportRemoteDataSource {
    var online = true
    val sent = mutableListOf<SendSupportMessageRequest>()
    val inbox = mutableListOf<SupportMessageResponse>()
    var requestedAfter: Long? = null
    private var nextId = 100L

    override suspend fun getMessages(afterId: Long): Try<List<SupportMessageResponse>> {
        if (!online) return Try.failure(IllegalStateException("offline"))
        requestedAfter = afterId
        return Try.success(inbox.filter { it.id > afterId })
    }

    override suspend fun send(request: SendSupportMessageRequest): Try<SupportMessageResponse> {
        if (!online) return Try.failure(IllegalStateException("offline"))
        sent += request
        val stored = inbox.firstOrNull { it.clientMessageId == request.clientMessageId }
            ?: SupportMessageResponse(nextId++, "USER", request.body, request.clientMessageId, 1L).also { inbox += it }
        return Try.success(stored)
    }
}

private object FixedClock : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(5_000)
}

class SupportRepositoryImplTest {

    private val local = FakeSupportLocal()
    private val remote = FakeSupportRemote()
    private val repository = SupportRepositoryImpl(local, remote, FakeSupportReadState(local), FixedClock, Random(1))

    private suspend fun messages() = repository.observeMessages().first()

    @Test
    fun `send stores the message and replaces it with the server copy`() = runTest {
        val result = repository.send("Hello")

        assertTrue(result.isSuccess)
        val message = messages().single()
        assertEquals(SupportDeliveryStatus.SENT, message.status)
        assertEquals(SupportSender.USER, message.sender)
        assertEquals("Hello", message.body)
        assertEquals(message.id, remote.sent.single().clientMessageId)
        assertEquals(100L, local.maxServerId())
    }

    @Test
    fun `send while offline keeps the message as failed`() = runTest {
        remote.online = false

        val result = repository.send("Hello")

        assertTrue(result.isSuccess, "storing locally succeeded")
        assertEquals(SupportDeliveryStatus.FAILED, messages().single().status)
    }

    @Test
    fun `retry sends a failed message once back online`() = runTest {
        remote.online = false
        repository.send("Hello")
        val id = messages().single().id
        remote.online = true

        repository.retry(id)

        assertEquals(SupportDeliveryStatus.SENT, messages().single().status)
        assertEquals(1, remote.sent.size)
    }

    @Test
    fun `refresh pulls only newer messages and stores admin replies`() = runTest {
        repository.send("Hello")
        remote.inbox += SupportMessageResponse(200, "ADMIN", "Hi there", null, 2L)

        val result = repository.refresh()

        assertTrue(result.isSuccess)
        assertEquals(100L, remote.requestedAfter)
        assertEquals(listOf("Hello", "Hi there"), messages().map { it.body })
        assertEquals(SupportSender.ADMIN, messages().last().sender)
    }

    @Test
    fun `refresh resends unsent messages after pulling`() = runTest {
        remote.online = false
        repository.send("queued")
        remote.online = true

        repository.refresh()

        assertEquals(listOf(SupportDeliveryStatus.SENT), messages().map { it.status })
        assertEquals(listOf("queued"), remote.sent.map { it.body })
    }

    @Test
    fun `refresh settles a message whose send response was lost`() = runTest {
        remote.online = false
        repository.send("lost response")
        val id = messages().single().id
        // The server did store it: it comes back on the pull with the same client id
        remote.inbox += SupportMessageResponse(150, "USER", "lost response", id, 1L)
        remote.online = true

        repository.refresh()

        val message = messages().single()
        assertEquals(id, message.id)
        assertEquals(SupportDeliveryStatus.SENT, message.status)
        assertTrue(remote.sent.isEmpty(), "nothing left to resend")
    }

    @Test
    fun `refresh offline fails but keeps local messages`() = runTest {
        repository.send("Hello")
        remote.online = false

        assertTrue(repository.refresh().isFailure)
        assertEquals(1, messages().size)
    }

    private suspend fun unread() = repository.observeUnread().first()

    private suspend fun receiveReply(id: Long) {
        remote.inbox += SupportMessageResponse(id, "ADMIN", "Reply $id", null, 2L)
        repository.refresh()
    }

    @Test
    fun `observeUnread with only the user's own messages shows nothing unread`() = runTest {
        repository.send("Hello")

        assertEquals(SupportUnread(inChat = false, onSettings = false), unread())
    }

    @Test
    fun `observeUnread after an admin reply flags chat and settings`() = runTest {
        repository.send("Hello")
        receiveReply(200)

        assertEquals(SupportUnread(inChat = true, onSettings = true), unread())
    }

    @Test
    fun `markSeenInSettings clears only the settings dot`() = runTest {
        repository.send("Hello")
        receiveReply(200)

        repository.markSeenInSettings()

        assertEquals(SupportUnread(inChat = true, onSettings = false), unread())
    }

    @Test
    fun `markRead clears both dots until the next reply`() = runTest {
        repository.send("Hello")
        receiveReply(200)

        repository.markRead()
        assertEquals(SupportUnread(), unread())

        receiveReply(201)
        assertEquals(SupportUnread(inChat = true, onSettings = true), unread())
    }

    @Test
    fun `hasConversation is false until something is stored`() = runTest {
        assertEquals(false, repository.hasConversation().getOrNull())

        repository.send("Hello")

        assertEquals(true, repository.hasConversation().getOrNull())
    }
}
