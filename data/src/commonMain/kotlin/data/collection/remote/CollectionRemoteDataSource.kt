package data.collection.remote

import data.collection.remote.model.DownloadCollectionRequest
import data.collection.remote.model.VocabularyCollection
import data.collection.remote.model.VocabularyContentResponse
import data.core.network.client.ApiClient
import domain.common.Try
import domain.common.doOnFailure
import domain.common.map
import expects.logNetwork

/**
 * Remote data source for vocabulary collection operations
 * Handles fetching available collections and downloading collection content
 */
class CollectionRemoteDataSource(
    private val apiClient: ApiClient
) {

    suspend fun getAvailableCollections(): Try<List<VocabularyCollection>> =
        apiClient.get<List<VocabularyCollection>>("/github")
            .map { it ?: emptyList() }
            .doOnFailure { error ->
                logNetwork("CollectionRemoteDataSource", "Error getting collections: ${error.message}")
            }

    suspend fun downloadCollection(
        targetLanguage: String,
        originLanguage: String,
        fileName: String
    ): Try<VocabularyContentResponse> =
        apiClient.postNotNull<VocabularyContentResponse>(
            path = "/collections/download",
            body = DownloadCollectionRequest(targetLanguage, originLanguage, fileName)
        ).doOnFailure { error ->
            logNetwork("CollectionRemoteDataSource", "Error downloading collection: ${error.message}")
        }
}
