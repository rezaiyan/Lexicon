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

class RenameTagUseCaseTest {

    private val renamedTag = Tag(id = 1L, name = "Deutsch", wordCount = 2L, createdAt = 0L, updatedAt = 0L)
    private var renameResult: Try<Tag> = Try.success(renamedTag)
    private var capturedId: Long? = null
    private var capturedName: String? = null

    private fun fakeRepo() = object : ITagRepository {
        override fun getTags(): Flow<List<Tag>> = flowOf(emptyList())
        override fun getTagsByLevel(): Flow<Map<Int, List<Tag>>> = flowOf(emptyMap())
        override fun getDueTags(): Flow<List<Tag>> = flowOf(emptyList())
        override suspend fun createTag(name: String): Try<Tag> = Try.success(renamedTag)
        override suspend fun renameTag(id: Long, name: String): Try<Tag> {
            capturedId = id
            capturedName = name
            return renameResult
        }
        override suspend fun deleteTag(id: Long): Try<Unit> = Try.success(Unit)
        override suspend fun assignWordTags(wordId: Long, tagIds: List<Long>): Try<Unit> = Try.success(Unit)
        override suspend fun batchAssignWordTags(wordIds: List<Long>, tagIds: List<Long>): Try<Unit> = Try.success(Unit)
        override suspend fun syncTagsFromRemote(): Try<Unit> = Try.success(Unit)
    }

    @Test
    fun `returns renamed tag on success`() = runTest {
        val useCase = RenameTagUseCase(fakeRepo())

        val result = useCase(RenameTagParams(id = 1L, name = "Deutsch"))

        assertTrue(result.isSuccess)
        assertEquals(renamedTag, result.getOrThrow())
    }

    @Test
    fun `passes id and name to repository`() = runTest {
        val useCase = RenameTagUseCase(fakeRepo())

        useCase(RenameTagParams(id = 42L, name = "NewName"))

        assertEquals(42L, capturedId)
        assertEquals("NewName", capturedName)
    }

    @Test
    fun `propagates failure from repository`() = runTest {
        renameResult = Try.failure(RuntimeException("Tag not found"))
        val useCase = RenameTagUseCase(fakeRepo())

        val result = useCase(RenameTagParams(id = 1L, name = "Deutsch"))

        assertTrue(result.isFailure)
        assertIs<RuntimeException>(result.exceptionOrNull())
        assertEquals("Tag not found", result.exceptionOrNull()?.message)
    }
}
