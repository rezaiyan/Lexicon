package domain.collection.repository

import domain.collection.model.CollectionContent
import domain.collection.model.VocabularyCollection
import domain.common.Try

interface ICollectionRepository {
    suspend fun getAvailableCollections(): Try<List<VocabularyCollection>>
    suspend fun downloadCollection(
        targetLanguage: String,
        originLanguage: String,
        fileName: String
    ): Try<CollectionContent>
}
