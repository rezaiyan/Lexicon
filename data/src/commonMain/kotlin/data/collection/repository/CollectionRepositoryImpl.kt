package data.collection.repository

import data.collection.mapper.toDomain
import data.collection.remote.CollectionRemoteDataSource
import domain.collection.model.CollectionContent
import domain.collection.model.VocabularyCollection
import domain.collection.repository.ICollectionRepository
import domain.common.Try
import domain.common.map

class CollectionRepositoryImpl(
    private val collectionRemoteDataSource: CollectionRemoteDataSource
) : ICollectionRepository {

    override suspend fun getAvailableCollections(): Try<List<VocabularyCollection>> {
        return collectionRemoteDataSource.getAvailableCollections()
            .map { collections -> collections.map { it.toDomain() } }
    }

    override suspend fun downloadCollection(
        targetLanguage: String,
        originLanguage: String,
        fileName: String
    ): Try<CollectionContent> {
        return collectionRemoteDataSource.downloadCollection(targetLanguage, originLanguage, fileName)
            .map { it.toDomain() }
    }
}
