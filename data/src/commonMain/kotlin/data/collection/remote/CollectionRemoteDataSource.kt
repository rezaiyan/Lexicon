package data.collection.remote

import data.collection.remote.model.DownloadCollectionRequest
import data.collection.remote.model.VocabularyCollection
import data.collection.remote.model.VocabularyContentResponse
import data.core.network.client.ApiClient
import expects.logNetwork

/**
 * Remote data source for vocabulary collection operations
 * Handles fetching available collections and downloading collection content
 */
class CollectionRemoteDataSource(
    private val apiClient: ApiClient
) {

    suspend fun getAvailableCollections(): Result<List<VocabularyCollection>> =
        apiClient.get<List<VocabularyCollection>>("/github")
            .map { it ?: emptyList() }
            .onFailure { error ->
                logNetwork("CollectionRemoteDataSource", "Error getting collections: ${error.message}")
            }

    suspend fun downloadCollection(
        targetLanguage: String,
        originLanguage: String,
        fileName: String
    ): Result<VocabularyContentResponse> =
        apiClient.postNotNull<VocabularyContentResponse>(
            path = "/collections/download",
            body = DownloadCollectionRequest(targetLanguage, originLanguage, fileName)
        ).onFailure { error ->
            logNetwork("CollectionRemoteDataSource", "Error downloading collection: ${error.message}")
        }
}

