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

class DeleteTagUseCaseTest {

    private var deleteResult: Try<Unit> = Try.success(Unit)
    private var capturedId: Long? = null

    private fun fakeRepo() = object : ITagRepository {
        override fun getTags(): Flow<List<Tag>> = flowOf(emptyList())
        override suspend fun createTag(name: String): Try<Tag> =
            Try.success(Tag(id = 1L, name = "", wordCount = 0L, createdAt = 0L, updatedAt = 0L))
        override suspend fun renameTag(id: Long, name: String): Try<Tag> =
            Try.success(Tag(id = 1L, name = "", wordCount = 0L, createdAt = 0L, updatedAt = 0L))
        override suspend fun deleteTag(id: Long): Try<Unit> {
            capturedId = id
            return deleteResult
        }
        override suspend fun assignWordTags(wordId: Long, tagIds: List<Long>): Try<Unit> = Try.success(Unit)
        override suspend fun addTagToWord(wordId: Long, tagId: Long): Try<Unit> = Try.success(Unit)
        override suspend fun syncTagsFromRemote(): Try<Unit> = Try.success(Unit)
    }

    @Test
    fun `returns success on successful deletion`() = runTest {
        val useCase = DeleteTagUseCase(fakeRepo())

        val result = useCase(1L)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `passes tag id to repository`() = runTest {
        val useCase = DeleteTagUseCase(fakeRepo())

        useCase(99L)

        assertEquals(99L, capturedId)
    }

    @Test
    fun `propagates failure from repository`() = runTest {
        deleteResult = Try.failure(RuntimeException("Cannot delete tag with words"))
        val useCase = DeleteTagUseCase(fakeRepo())

        val result = useCase(1L)

        assertTrue(result.isFailure)
        assertIs<RuntimeException>(result.exceptionOrNull())
        assertEquals("Cannot delete tag with words", result.exceptionOrNull()?.message)
    }
}
