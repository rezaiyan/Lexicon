package data.collection.repository

import data.collection.mapper.toDomain
import data.collection.remote.CollectionRemoteDataSource
import domain.collection.model.CollectionContent
import domain.collection.model.VocabularyCollection
import domain.collection.repository.ICollectionRepository

class CollectionRepositoryImpl(
    private val collectionRemoteDataSource: CollectionRemoteDataSource
) : ICollectionRepository {

    override suspend fun getAvailableCollections(): Result<List<VocabularyCollection>> {
        return collectionRemoteDataSource.getAvailableCollections()
            .map { collections -> collections.map { it.toDomain() } }
    }

    override suspend fun downloadCollection(
        targetLanguage: String,
        originLanguage: String,
        fileName: String
    ): Result<CollectionContent> {
        return collectionRemoteDataSource.downloadCollection(targetLanguage, originLanguage, fileName)
            .map { it.toDomain() }
    }
}
