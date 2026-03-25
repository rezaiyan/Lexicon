package data.analytics.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import data.core.database.LexiconQueries

class LexiconAnalyticsLocalQueue(
    private val queries: LexiconQueries,
) : IAnalyticsLocalQueue {
    override suspend fun insertRequest(requestJson: String, createdAt: Long) {
        queries.insertAnalyticsSyncRequest(requestJson = requestJson, createdAt = createdAt)
    }

    override suspend fun getAllRequests(): List<String> =
        queries.getAllAnalyticsSyncRequests().awaitAsList().map { it.requestJson }

    override suspend fun clearQueue() {
        queries.clearAnalyticsSyncQueue()
    }
}
