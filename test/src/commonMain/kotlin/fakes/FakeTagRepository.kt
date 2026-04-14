package fakes

import core.common.Try
import domain.tag.model.Tag
import domain.tag.repository.ITagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeTagRepository : ITagRepository {
    var syncTagsFromRemoteCalled = false
    var syncTagsFromRemoteResult: Try<Unit> = Try.success(Unit)

    override fun getTags(): Flow<List<Tag>> = flowOf(emptyList())
    override fun getTagsByLevel(): Flow<Map<Int, List<Tag>>> = flowOf(emptyMap())
    override fun getDueTags(): Flow<List<Tag>> = flowOf(emptyList())
    override suspend fun createTag(name: String): Try<Tag> = throw NotImplementedError()
    override suspend fun renameTag(id: Long, name: String): Try<Tag> = throw NotImplementedError()
    override suspend fun deleteTag(id: Long): Try<Unit> = throw NotImplementedError()
    override suspend fun assignWordTags(wordId: Long, tagIds: List<Long>): Try<Unit> = throw NotImplementedError()
    override suspend fun batchAssignWordTags(wordIds: List<Long>, tagIds: List<Long>): Try<Unit> = throw NotImplementedError()

    override suspend fun syncTagsFromRemote(): Try<Unit> {
        syncTagsFromRemoteCalled = true
        return syncTagsFromRemoteResult
    }
}
