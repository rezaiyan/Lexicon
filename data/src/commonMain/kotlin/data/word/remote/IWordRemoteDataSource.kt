package data.word.remote

import data.word.remote.model.BatchUpdateLanguagesRequest
import data.word.remote.model.RemoteWord
import core.common.Try

interface IWordRemoteDataSource {
    suspend fun getWords(updatedAfter: Long? = null): Try<List<RemoteWord>>
    /** Returns the words as saved by the server (with server ids); empty from servers that predate this. */
    suspend fun upsertWords(words: List<RemoteWord>): Try<List<RemoteWord>>
    suspend fun updateWord(id: Long, word: RemoteWord): Try<Unit>
    suspend fun deleteWord(id: Long): Try<Unit>
    suspend fun deleteWords(ids: List<Long>): Try<Unit>
    suspend fun batchUpdateLanguages(request: BatchUpdateLanguagesRequest): Try<Unit>
}
