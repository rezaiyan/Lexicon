package data.support.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import data.core.database.LexiconQueries
import data.core.database.SupportMessageEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

interface ISupportLocalDataSource {
    fun observe(): Flow<List<SupportMessageEntity>>
    suspend fun maxServerId(): Long
    suspend fun unsent(): List<SupportMessageEntity>
    suspend fun find(localId: String): SupportMessageEntity?
    suspend fun upsert(messages: List<SupportMessageEntity>)
    suspend fun setStatus(localId: String, status: String)
    suspend fun count(): Long
}

class SupportLocalDataSource(
    private val queries: LexiconQueries,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ISupportLocalDataSource {

    override fun observe(): Flow<List<SupportMessageEntity>> =
        queries.observeSupportMessages().asFlow().mapToList(dispatcher)

    override suspend fun maxServerId(): Long =
        queries.maxSupportServerId().awaitAsOneOrNull()?.maxId ?: 0L

    override suspend fun unsent(): List<SupportMessageEntity> =
        queries.unsentSupportMessages().awaitAsList()

    override suspend fun find(localId: String): SupportMessageEntity? =
        queries.supportMessageById(localId).awaitAsOneOrNull()

    override suspend fun upsert(messages: List<SupportMessageEntity>) {
        queries.transaction {
            messages.forEach {
                queries.upsertSupportMessage(it.localId, it.serverId, it.sender, it.body, it.createdAt, it.status)
            }
        }
    }

    override suspend fun setStatus(localId: String, status: String) {
        queries.setSupportMessageStatus(status, localId)
    }

    override suspend fun count(): Long = queries.countSupportMessages().awaitAsOne()
}
