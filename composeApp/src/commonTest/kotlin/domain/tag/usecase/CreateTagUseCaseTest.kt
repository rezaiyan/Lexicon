package domain.tag.usecase

import core.common.Try
import domain.tag.model.Tag
import domain.tag.repository.ITagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import core.common.exceptionOrNull
import core.common.getOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CreateTagUseCaseTest {

    private val createdTag = Tag(id = 1L, name = "Spanish", wordCount = 0L, createdAt = 0L, updatedAt = 0L)
    private var createResult: Try<Tag> = Try.success(createdTag)
    private var capturedName: String? = null

    private fun fakeRepo() = object : ITagRepository {
        override fun getTags(): Flow<List<Tag>> = flowOf(emptyList())
        override fun getTagsByLevel(): Flow<Map<Int, List<Tag>>> = flowOf(emptyMap())
        override fun getDueTags(): Flow<List<Tag>> = flowOf(emptyList())
        override suspend fun createTag(name: String): Try<Tag> {
            capturedName = name
            return createResult
        }
        override suspend fun renameTag(id: Long, name: String): Try<Tag> = Try.success(createdTag)
        override suspend fun deleteTag(id: Long): Try<Unit> = Try.success(Unit)
        override suspend fun assignWordTags(wordId: Long, tagIds: List<Long>): Try<Unit> = Try.success(Unit)
        override suspend fun batchAssignWordTags(wordIds: List<Long>, tagIds: List<Long>): Try<Unit> = Try.success(Unit)
        override suspend fun syncTagsFromRemote(): Try<Unit> = Try.success(Unit)
    }

    @Test
    fun `returns created tag on success`() = runTest {
        val useCase = CreateTagUseCase(fakeRepo())

        val result = useCase("Spanish")

        assertTrue(result.isSuccess)
        assertEquals(createdTag, result.getOrThrow())
    }

    @Test
    fun `passes name to repository`() = runTest {
        val useCase = CreateTagUseCase(fakeRepo())

        useCase("Travel")

        assertEquals("Travel", capturedName)
    }

    @Test
    fun `propagates failure from repository`() = runTest {
        createResult = Try.failure(RuntimeException("Name already exists"))
        val useCase = CreateTagUseCase(fakeRepo())

        val result = useCase("Spanish")

        assertTrue(result.isFailure)
        assertIs<RuntimeException>(result.exceptionOrNull())
        assertEquals("Name already exists", result.exceptionOrNull()?.message)
    }
}
