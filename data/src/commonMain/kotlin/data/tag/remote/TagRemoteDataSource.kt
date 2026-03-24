package data.tag.remote

import core.common.Try
import core.common.map
import data.core.network.client.ApiClient
import data.tag.remote.model.CreateTagPayload
import data.tag.remote.model.RemoteTag
import data.tag.remote.model.RenameTagPayload
import data.tag.remote.model.UpdateWordTagsPayload

class TagRemoteDataSource(
    private val apiClient: ApiClient
) : ITagRemoteDataSource {

    override suspend fun getTags(): Try<List<RemoteTag>> =
        apiClient.get<List<RemoteTag>>("/tags")
            .map { it ?: emptyList() }

    override suspend fun createTag(name: String): Try<RemoteTag> =
        apiClient.postNotNull("/tags", CreateTagPayload(name))

    override suspend fun renameTag(id: Long, name: String): Try<RemoteTag> =
        apiClient.putNotNull("/tags/$id", RenameTagPayload(name))

    override suspend fun deleteTag(id: Long): Try<Unit> =
        apiClient.delete("/tags/$id")

    override suspend fun updateWordTags(wordId: Long, tagIds: List<Long>): Try<Unit> =
        apiClient.putUnit("/words/$wordId/tags", UpdateWordTagsPayload(tagIds))
}
