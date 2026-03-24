package data.tag.remote

import core.common.Try
import data.tag.remote.model.RemoteTag

interface ITagRemoteDataSource {
    suspend fun getTags(): Try<List<RemoteTag>>
    suspend fun createTag(name: String): Try<RemoteTag>
    suspend fun renameTag(id: Long, name: String): Try<RemoteTag>
    suspend fun deleteTag(id: Long): Try<Unit>
    suspend fun updateWordTags(wordId: Long, tagIds: List<Long>): Try<Unit>
}
