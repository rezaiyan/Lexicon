package data.collection.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import data.core.database.DownloadedCollectionEntity
import data.core.database.DownloadedCollectionEntityData
import data.core.database.LexiconQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class DownloadedCollectionRepository(
    private val queries: LexiconQueries
) {

    suspend fun insert(collection: DownloadedCollectionEntityData) {
        queries.insertDownloadedCollection(
            id = collection.id,
            targetLanguage = collection.targetLanguage,
            originLanguage = collection.originLanguage,
            title = collection.title,
            fileName = collection.fileName,
            path = collection.path,
            downloadedAt = collection.downloadedAt
        )
    }

    suspend fun get(targetLanguage: String, originLanguage: String, fileName: String): DownloadedCollectionEntity? {
        return queries.getDownloadedCollection(targetLanguage, originLanguage, fileName)
            .awaitAsOneOrNull()
    }

    fun getAll(): Flow<List<DownloadedCollectionEntity>> =
        queries.getAllDownloadedCollections().asFlow().mapToList(Dispatchers.Default)
}
