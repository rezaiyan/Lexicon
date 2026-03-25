package domain.tag.usecase

import core.common.Try
import domain.tag.model.Tag
import domain.tag.repository.ITagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import core.common.exceptionOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AssignWordTagsUseCaseTest {

    private var assignResult: Try<Unit> = Try.success(Unit)
    private var capturedWordId: Long? = null
    private var capturedTagIds: List<Long>? = null

    private fun fakeRepo() = object : ITagRepository {
        override fun getTags(): Flow<List<Tag>> = flowOf(emptyList())
        override fun getTagsByLevel(): Flow<Map<Int, List<Tag>>> = flowOf(emptyMap())
        override fun getDueTags(): Flow<List<Tag>> = flowOf(emptyList())
        override suspend fun createTag(name: String): Try<Tag> =
            Try.success(Tag(id = 1L, name = "", wordCount = 0L, createdAt = 0L, updatedAt = 0L))
        override suspend fun renameTag(id: Long, name: String): Try<Tag> =
            Try.success(Tag(id = 1L, name = "", wordCount = 0L, createdAt = 0L, updatedAt = 0L))
        override suspend fun deleteTag(id: Long): Try<Unit> = Try.success(Unit)
        override suspend fun assignWordTags(wordId: Long, tagIds: List<Long>): Try<Unit> {
            capturedWordId = wordId
            capturedTagIds = tagIds
            return assignResult
        }
        override suspend fun batchAssignWordTags(wordIds: List<Long>, tagIds: List<Long>): Try<Unit> = Try.success(Unit)
        override suspend fun syncTagsFromRemote(): Try<Unit> = Try.success(Unit)
    }

    @Test
    fun `returns success when assignment succeeds`() = runTest {
        val useCase = AssignWordTagsUseCase(fakeRepo())

        val result = useCase(AssignWordTagsParams(wordId = 10L, tagIds = listOf(1L, 2L)))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `passes wordId and tagIds to repository`() = runTest {
        val useCase = AssignWordTagsUseCase(fakeRepo())

        useCase(AssignWordTagsParams(wordId = 42L, tagIds = listOf(5L, 7L, 9L)))

        assertEquals(42L, capturedWordId)
        assertEquals(listOf(5L, 7L, 9L), capturedTagIds)
    }

    @Test
    fun `passes empty tag list to repository`() = runTest {
        val useCase = AssignWordTagsUseCase(fakeRepo())

        useCase(AssignWordTagsParams(wordId = 1L, tagIds = emptyList()))

        assertEquals(1L, capturedWordId)
        assertEquals(emptyList(), capturedTagIds)
    }

    @Test
    fun `propagates failure from repository`() = runTest {
        assignResult = Try.failure(RuntimeException("Word not found"))
        val useCase = AssignWordTagsUseCase(fakeRepo())

        val result = useCase(AssignWordTagsParams(wordId = 1L, tagIds = listOf(1L)))

        assertTrue(result.isFailure)
        assertIs<RuntimeException>(result.exceptionOrNull())
        assertEquals("Word not found", result.exceptionOrNull()?.message)
    }
}
