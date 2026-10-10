package data.insights.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import data.core.database.LexiconQueries
import data.insights.remote.InsightsScreenDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

data class StoredInsightsScreen(val dto: InsightsScreenDto, val fetchedAtMs: Long)

interface IInsightsScreenLocalDataSource {
    fun observe(): Flow<StoredInsightsScreen?>
    suspend fun write(dto: InsightsScreenDto, fetchedAtMs: Long)
    fun observeDismissed(): Flow<Set<String>>

    /** Records [cardId] and drops ids from other days ([today] is ISO yyyy-MM-dd). */
    suspend fun dismiss(cardId: String, today: String)
    suspend fun clear()
}

class InsightsScreenLocalDataSource(
    private val queries: LexiconQueries,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : IInsightsScreenLocalDataSource {

    /** A row written by an older app version that no longer parses is treated as absent. */
    override fun observe(): Flow<StoredInsightsScreen?> =
        queries.getInsightsScreenCache().asFlow().mapToOneOrNull(dispatcher).map { row ->
            row?.let { decode(it.json)?.let { dto -> StoredInsightsScreen(dto, it.fetched_at_ms) } }
        }

    override suspend fun write(dto: InsightsScreenDto, fetchedAtMs: Long) {
        queries.saveInsightsScreenCache(json.encodeToString(InsightsScreenDto.serializer(), dto), fetchedAtMs)
    }

    override fun observeDismissed(): Flow<Set<String>> =
        queries.selectDismissedCoachCards().asFlow().mapToList(dispatcher).map { it.toSet() }

    override suspend fun dismiss(cardId: String, today: String) {
        queries.transaction {
            queries.pruneDismissedCoachCards(today)
            queries.insertDismissedCoachCard(cardId)
        }
    }

    override suspend fun clear() {
        queries.transaction {
            queries.clearInsightsScreenCache()
            queries.clearDismissedCoachCards()
        }
    }

    private fun decode(raw: String): InsightsScreenDto? =
        // Decode boundary, not control flow: SerializationException extends IllegalArgumentException.
        try {
            json.decodeFromString(InsightsScreenDto.serializer(), raw)
        } catch (_: IllegalArgumentException) {
            null
        }
}
