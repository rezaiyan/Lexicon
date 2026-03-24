package domain.tag.repository

import core.common.Try
import domain.tag.model.Tag
import kotlinx.coroutines.flow.Flow

interface ITagRepository {
    fun getTags(): Flow<List<Tag>>
    suspend fun createTag(name: String): Try<Tag>
    suspend fun renameTag(id: Long, name: String): Try<Tag>
    suspend fun deleteTag(id: Long): Try<Unit>
    suspend fun assignWordTags(wordId: Long, tagIds: List<Long>): Try<Unit>
    suspend fun addTagToWord(wordId: Long, tagId: Long): Try<Unit>
    suspend fun syncTagsFromRemote(): Try<Unit>
}
