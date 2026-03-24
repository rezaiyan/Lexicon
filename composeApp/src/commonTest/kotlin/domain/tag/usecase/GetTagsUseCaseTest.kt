package domain.tag.usecase

import core.common.Try
import domain.tag.model.Tag
import domain.tag.repository.ITagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetTagsUseCaseTest {

    private val tagA = Tag(id = 1L, name = "Spanish", wordCount = 5L, createdAt = 0L, updatedAt = 0L)
    private val tagB = Tag(id = 2L, name = "French", wordCount = 3L, createdAt = 0L, updatedAt = 0L)

    private fun fakeRepo(tags: List<Tag>) = object : ITagRepository {
        override fun getTags(): Flow<List<Tag>> = flowOf(tags)
        override suspend fun createTag(name: String): Try<Tag> = Try.success(tagA)
        override suspend fun renameTag(id: Long, name: String): Try<Tag> = Try.success(tagA)
        override suspend fun deleteTag(id: Long): Try<Unit> = Try.success(Unit)
        override suspend fun assignWordTags(wordId: Long, tagIds: List<Long>): Try<Unit> = Try.success(Unit)
        override suspend fun addTagToWord(wordId: Long, tagId: Long): Try<Unit> = Try.success(Unit)
        override suspend fun syncTagsFromRemote(): Try<Unit> = Try.success(Unit)
    }

    @Test
    fun `invoke returns flow of tags from repository`() = runTest {
        val tags = listOf(tagA, tagB)
        val useCase = GetTagsUseCase(fakeRepo(tags))

        val emitted = useCase().first()

        assertEquals(tags, emitted)
    }

    @Test
    fun `invoke via Unit params returns same flow`() = runTest {
        val tags = listOf(tagA)
        val useCase = GetTagsUseCase(fakeRepo(tags))

        val emitted = useCase(Unit).first()

        assertEquals(tags, emitted)
    }

    @Test
    fun `returns empty list when repository has no tags`() = runTest {
        val useCase = GetTagsUseCase(fakeRepo(emptyList()))

        val emitted = useCase().first()

        assertEquals(emptyList(), emitted)
    }
}
