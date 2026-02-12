package data.collection.repository

import data.core.database.DownloadedCollectionEntity
import data.core.database.LexiconDao

class DownloadedCollectionRepository(
    private val dao: LexiconDao
) {

    suspend fun insert(collection: DownloadedCollectionEntity) {
        dao.insertDownloadedCollection(collection)
    }

    suspend fun get(targetLanguage: String, originLanguage: String, fileName: String): DownloadedCollectionEntity? {
        return dao.getDownloadedCollection(targetLanguage, originLanguage, fileName)
    }

    fun getAll() = dao.getAllDownloadedCollections()
}

