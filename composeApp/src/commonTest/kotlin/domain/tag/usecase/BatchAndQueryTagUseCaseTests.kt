package domain.tag.usecase

import app.cash.turbine.test
import core.common.Try
import core.common.exceptionOrNull
import core.common.getOrNull
import domain.tag.model.Tag
import domain.tag.repository.ITagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BatchAndQueryTagUseCaseTests {

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun tag(id: Long, name: String) =
        Tag(id = id, name = name, wordCount = 0L, createdAt = 0L, updatedAt = 0L)

    private var batchAssignResult: Try<Unit> = Try.success(Unit)
    private var capturedBatchWordIds: List<Long>? = null
    private var capturedBatchTagIds: List<Long>? = null

    private var dueTagsFlow: Flow<List<Tag>> = flowOf(emptyList())
    private var tagsByLevelFlow: Flow<Map<Int, List<Tag>>> = flowOf(emptyMap())

    private fun fakeRepo() = object : ITagRepository {
        override fun getTags(): Flow<List<Tag>> = flowOf(emptyList())
        override fun getTagsByLevel(): Flow<Map<Int, List<Tag>>> = tagsByLevelFlow
        override fun getDueTags(): Flow<List<Tag>> = dueTagsFlow
        override suspend fun createTag(name: String): Try<Tag> =
            Try.success(tag(1L, name))
        override suspend fun renameTag(id: Long, name: String): Try<Tag> =
            Try.success(tag(id, name))
        override suspend fun deleteTag(id: Long): Try<Unit> = Try.success(Unit)
        override suspend fun assignWordTags(wordId: Long, tagIds: List<Long>): Try<Unit> =
            Try.success(Unit)
        override suspend fun batchAssignWordTags(wordIds: List<Long>, tagIds: List<Long>): Try<Unit> {
            capturedBatchWordIds = wordIds
            capturedBatchTagIds = tagIds
            return batchAssignResult
        }
        override suspend fun syncTagsFromRemote(): Try<Unit> = Try.success(Unit)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BatchAssignTagsUseCase
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `BatchAssignTagsUseCase returns wordIds size on success`() = runTest {
        val useCase = BatchAssignTagsUseCase(fakeRepo())

        val result = useCase(BatchAssignTagsParams(wordIds = listOf(1, 2, 3), tagIds = listOf(10L, 20L)))

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
    }

    @Test
    fun `BatchAssignTagsUseCase maps Int wordIds to Long when calling repo`() = runTest {
        val useCase = BatchAssignTagsUseCase(fakeRepo())

        useCase(BatchAssignTagsParams(wordIds = listOf(5, 10, 15), tagIds = listOf(1L, 2L)))

        assertEquals(listOf(5L, 10L, 15L), capturedBatchWordIds)
        assertEquals(listOf(1L, 2L), capturedBatchTagIds)
    }

    @Test
    fun `BatchAssignTagsUseCase propagates failure from repository`() = runTest {
        batchAssignResult = Try.failure(RuntimeException("Batch assign failed"))
        val useCase = BatchAssignTagsUseCase(fakeRepo())

        val result = useCase(BatchAssignTagsParams(wordIds = listOf(1, 2), tagIds = listOf(1L)))

        assertTrue(result.isFailure)
        assertIs<RuntimeException>(result.exceptionOrNull())
        assertEquals("Batch assign failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `BatchAssignTagsUseCase returns 0 for empty wordIds`() = runTest {
        val useCase = BatchAssignTagsUseCase(fakeRepo())

        val result = useCase(BatchAssignTagsParams(wordIds = emptyList(), tagIds = listOf(1L)))

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GetDueTagsUseCase
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `GetDueTagsUseCase emits tags from repository getDueTags flow`() = runTest {
        val expectedTags = listOf(tag(1L, "due-tag-1"), tag(2L, "due-tag-2"))
        dueTagsFlow = flowOf(expectedTags)
        val useCase = GetDueTagsUseCase(fakeRepo())

        useCase(Unit).test {
            assertEquals(expectedTags, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `GetDueTagsUseCase emits empty list when no due tags`() = runTest {
        dueTagsFlow = flowOf(emptyList())
        val useCase = GetDueTagsUseCase(fakeRepo())

        useCase(Unit).test {
            assertEquals(emptyList(), awaitItem())
            awaitComplete()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GetTagsByLevelUseCase
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `GetTagsByLevelUseCase emits map from repository getTagsByLevel flow`() = runTest {
        val expectedMap = mapOf(
            1 to listOf(tag(1L, "beginner")),
            2 to listOf(tag(2L, "intermediate")),
        )
        tagsByLevelFlow = flowOf(expectedMap)
        val useCase = GetTagsByLevelUseCase(fakeRepo())

        useCase(Unit).test {
            assertEquals(expectedMap, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `GetTagsByLevelUseCase emits empty map when no data`() = runTest {
        tagsByLevelFlow = flowOf(emptyMap())
        val useCase = GetTagsByLevelUseCase(fakeRepo())

        useCase(Unit).test {
            assertEquals(emptyMap(), awaitItem())
            awaitComplete()
        }
    }
}
