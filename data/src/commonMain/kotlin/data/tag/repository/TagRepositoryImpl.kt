package data.tag.repository

import core.common.Try
import core.common.getOrThrow
import data.tag.local.ITagLocalDataSource
import data.tag.mapper.toDomain
import data.tag.remote.ITagRemoteDataSource
import domain.tag.model.Tag
import domain.tag.repository.ITagRepository
import kotlinx.coroutines.flow.Flow

class TagRepositoryImpl(
    private val localDataSource: ITagLocalDataSource,
    private val remoteDataSource: ITagRemoteDataSource
) : ITagRepository {

    override fun getTags(): Flow<List<Tag>> = localDataSource.getTags()

    override fun getTagsByLevel(): Flow<Map<Int, List<Tag>>> = localDataSource.getTagsByLevel()

    override fun getDueTags(): Flow<List<Tag>> = localDataSource.getDueTags()

    override suspend fun createTag(name: String): Try<Tag> = Try {
        val remote = remoteDataSource.createTag(name).getOrThrow()
        val tag = remote.toDomain()
        localDataSource.insertOrReplaceTag(tag.id, tag.name, tag.createdAt, tag.updatedAt)
        tag
    }

    override suspend fun renameTag(id: Long, name: String): Try<Tag> = Try {
        val remote = remoteDataSource.renameTag(id, name).getOrThrow()
        val tag = remote.toDomain()
        localDataSource.insertOrReplaceTag(tag.id, tag.name, tag.createdAt, tag.updatedAt)
        tag
    }

    override suspend fun deleteTag(id: Long): Try<Unit> = Try {
        remoteDataSource.deleteTag(id).getOrThrow()
        localDataSource.deleteTag(id)
    }

    override suspend fun assignWordTags(wordId: Long, tagIds: List<Long>): Try<Unit> = Try {
        remoteDataSource.updateWordTags(wordId, tagIds).getOrThrow()
        localDataSource.setWordTags(wordId, tagIds)
    }

    override suspend fun batchAssignWordTags(wordIds: List<Long>, tagIds: List<Long>): Try<Unit> = Try {
        remoteDataSource.batchUpdateWordTags(wordIds, tagIds).getOrThrow()
        localDataSource.batchSetWordTags(wordIds, tagIds)
    }

    override suspend fun syncTagsFromRemote(): Try<Unit> = Try {
        val remoteTags = remoteDataSource.getTags().getOrThrow()
        localDataSource.replaceAllTags(remoteTags.map { it.toDomain() })
    }
}
