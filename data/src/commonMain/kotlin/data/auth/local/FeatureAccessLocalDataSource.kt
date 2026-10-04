package data.auth.local

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import data.core.database.LexiconQueries
import domain.auth.model.FeatureAccessResponse
import kotlinx.serialization.json.Json

/** Device copy of the last feature-access answer, used until the network answers again. */
interface IFeatureAccessLocalDataSource {
    suspend fun read(): FeatureAccessResponse?
    suspend fun write(featureAccess: FeatureAccessResponse)
    suspend fun clear()
}

class FeatureAccessLocalDataSourceImpl(
    private val queries: LexiconQueries,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : IFeatureAccessLocalDataSource {

    /** A row written by an older app version that no longer parses is treated as absent. */
    override suspend fun read(): FeatureAccessResponse? {
        val stored = queries.getFeatureAccessCache().awaitAsOneOrNull() ?: return null
        // SerializationException extends IllegalArgumentException.
        return try {
            json.decodeFromString<FeatureAccessResponse>(stored)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    override suspend fun write(featureAccess: FeatureAccessResponse) {
        queries.saveFeatureAccessCache(json.encodeToString(featureAccess))
    }

    override suspend fun clear() {
        queries.clearFeatureAccessCache()
    }
}
