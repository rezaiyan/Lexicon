package data.word.remote

import data.core.network.client.ApiClient
import data.word.remote.model.RemoteWord
import data.word.remote.model.UpsertWordsPayload
import domain.common.Try
import domain.common.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Remote data source for word operations
 * Handles all CRUD operations for words on the backend
 */
class WordRemoteDataSource(
    private val apiClient: ApiClient
) {

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

    fun deleteWords(ids: List<Long>): Flow<Try<Unit>> =
        if (ids.isEmpty()) {
            flow { emit(Try.success(Unit)) }
        } else {
            flow {
                emit(
                    apiClient.postUnit(
                        path = "/words/batch-delete",
                        body = mapOf("ids" to ids)
                    )
                )
            }
        }
}
