package domain.collection.repository

import domain.collection.model.CollectionContent
import domain.collection.model.VocabularyCollection

interface ICollectionRepository {
    suspend fun getAvailableCollections(): Result<List<VocabularyCollection>>
    suspend fun downloadCollection(
        targetLanguage: String,
        originLanguage: String,
        fileName: String
    ): Result<CollectionContent>
}
