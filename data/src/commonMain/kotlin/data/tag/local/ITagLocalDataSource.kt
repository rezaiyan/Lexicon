package data.tag.local

import domain.tag.model.Tag
import kotlinx.coroutines.flow.Flow

interface ITagLocalDataSource {
    fun getTags(): Flow<List<Tag>>
    suspend fun insertOrReplaceTag(id: Long, name: String, createdAt: Long, updatedAt: Long)
    suspend fun deleteTag(id: Long)
    suspend fun deleteAllTags()
    suspend fun replaceAllTags(tags: List<Tag>)
    suspend fun setWordTags(wordId: Long, tagIds: List<Long>)
    suspend fun addWordTag(wordId: Long, tagId: Long)
    suspend fun getTagIdsForWord(wordId: Long): List<Long>
}
