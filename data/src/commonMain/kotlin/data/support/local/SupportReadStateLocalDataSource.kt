package data.support.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import data.core.database.LexiconQueries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Client-only read markers for support replies, kept apart from the messages themselves. */
interface ISupportReadStateLocalDataSource {
    fun observeUnread(): Flow<SupportUnreadRow>
    suspend fun markRead()
    suspend fun markSeenInSettings()
}

/** Newest admin reply id and the two read markers; ids are server ids, 0 when none. */
data class SupportUnreadRow(val latestReplyId: Long, val readServerId: Long, val settingsSeenServerId: Long)

class SupportReadStateLocalDataSource(
    private val queries: LexiconQueries,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ISupportReadStateLocalDataSource {

    override fun observeUnread(): Flow<SupportUnreadRow> =
        queries.observeSupportUnread().asFlow().mapToOne(dispatcher).map {
            SupportUnreadRow(it.latestReplyId, it.readServerId, it.settingsSeenServerId)
        }

    override suspend fun markRead() {
        queries.transaction {
            queries.ensureSupportReadStateRow()
            queries.markSupportRead()
        }
    }

    override suspend fun markSeenInSettings() {
        queries.transaction {
            queries.ensureSupportReadStateRow()
            queries.markSupportSeenInSettings()
        }
    }
}
