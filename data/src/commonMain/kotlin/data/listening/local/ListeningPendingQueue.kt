package data.listening.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import data.core.database.LexiconQueries

/** A session waiting to sync: its client id and the serialized sync request. */
data class PendingListeningSession(val clientSessionId: String, val requestJson: String)

interface IListeningPendingQueue {
    suspend fun insert(session: PendingListeningSession, createdAt: Long)
    suspend fun getAll(): List<PendingListeningSession>
    suspend fun delete(clientSessionIds: Collection<String>)
}

class ListeningPendingQueue(
    private val queries: LexiconQueries,
) : IListeningPendingQueue {

    override suspend fun insert(session: PendingListeningSession, createdAt: Long) {
        queries.insertListeningPendingSession(session.clientSessionId, session.requestJson, createdAt)
    }

    override suspend fun getAll(): List<PendingListeningSession> =
        queries.selectAllListeningPendingSessions().awaitAsList()
            .map { PendingListeningSession(it.client_session_id, it.request_json) }

    override suspend fun delete(clientSessionIds: Collection<String>) {
        queries.deleteListeningPendingSessions(clientSessionIds)
    }
}
