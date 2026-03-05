package data.word.remote

import data.core.network.client.ApiClient
import data.word.remote.model.BatchUpdateLanguagesRequest
import data.word.remote.model.RemoteWord
import data.word.remote.model.UpsertWordsPayload
import core.common.Try
import core.common.map

/**
 * Remote data source for word operations
 * Handles all CRUD operations for words on the backend
 */
class WordRemoteDataSource(
    private val apiClient: ApiClient
) : IWordRemoteDataSource {

    suspend fun getWords(): Try<List<RemoteWord>> =
        apiClient.get<List<RemoteWord>>("/words")
            .map { it ?: emptyList() }

    suspend fun upsertWords(words: List<RemoteWord>): Try<Unit> =
        apiClient.postUnit(
            path = "/words",
            body = UpsertWordsPayload(words)
        )

    suspend fun updateWord(id: Long, word: RemoteWord): Try<Unit> =
        apiClient.patchUnit(
            path = "/words/$id",
            body = word
        )

    suspend fun deleteWord(id: Long): Try<Unit> =
        apiClient.delete("/words/$id")

    suspend fun deleteWords(ids: List<Long>): Try<Unit> =
        if (ids.isEmpty()) {
            Try.success(Unit)
        } else {
            apiClient.postUnit(
                path = "/words/batch-delete",
                body = mapOf("ids" to ids)
            )
        }

    suspend fun batchUpdateLanguages(request: BatchUpdateLanguagesRequest): Try<Unit> =
        apiClient.postUnit(
            path = "/words/batch-update",
            body = request
        )
}
