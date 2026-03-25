package feature.words

import core.common.Try
import domain.tag.model.Tag
import domain.tag.repository.ITagRepository
import domain.tag.usecase.AssignWordTagsUseCase
import domain.tag.usecase.GetTagsUseCase
import feature.words.model.WordTagAssignmentEffect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WordTagAssignmentViewModelTest : ViewModelTestBase() {

    private val tagA = Tag(id = 1L, name = "Spanish", wordCount = 5L, createdAt = 0L, updatedAt = 0L)
    private val tagB = Tag(id = 2L, name = "French", wordCount = 3L, createdAt = 0L, updatedAt = 0L)
    private val tagC = Tag(id = 3L, name = "German", wordCount = 1L, createdAt = 0L, updatedAt = 0L)

    private val tagsFlow = MutableStateFlow<List<Tag>>(emptyList())
    private var assignResult: Try<Unit> = Try.success(Unit)
    private var capturedWordId: Long? = null
    private var capturedTagIds: List<Long>? = null

    private fun fakeRepo() = object : ITagRepository {
        override fun getTags(): Flow<List<Tag>> = tagsFlow
        override fun getTagsByLevel(): Flow<Map<Int, List<Tag>>> = flowOf(emptyMap())
        override fun getDueTags(): Flow<List<Tag>> = flowOf(emptyList())
        override suspend fun createTag(name: String): Try<Tag> = Try.success(tagA)
        override suspend fun renameTag(id: Long, name: String): Try<Tag> = Try.success(tagA)
        override suspend fun deleteTag(id: Long): Try<Unit> = Try.success(Unit)
        override suspend fun assignWordTags(wordId: Long, tagIds: List<Long>): Try<Unit> {
            capturedWordId = wordId
            capturedTagIds = tagIds
            return assignResult
        }
        override suspend fun batchAssignWordTags(wordIds: List<Long>, tagIds: List<Long>): Try<Unit> = Try.success(Unit)
        override suspend fun syncTagsFromRemote(): Try<Unit> = Try.success(Unit)
    }

    private fun createViewModel() = WordTagAssignmentViewModel(
        getTagsUseCase = GetTagsUseCase(fakeRepo()),
        assignWordTagsUseCase = AssignWordTagsUseCase(fakeRepo()),
    )

    // --- initialize ---

    @Test
    fun `initialize sets wordId on state`() = runTest {
        val vm = createViewModel()

        vm.initialize(wordId = 7, currentTagIds = emptyList())

        assertEquals(7, vm.currentState.wordId)
    }

    @Test
    fun `initialize sets selectedTagIds from currentTagIds`() = runTest {
        val vm = createViewModel()

        vm.initialize(wordId = 1, currentTagIds = listOf(1L, 3L))

        assertEquals(setOf(1L, 3L), vm.currentState.selectedTagIds)
    }

    @Test
    fun `initialize loads tags from repository into state`() = runTest {
        tagsFlow.value = listOf(tagA, tagB)
        val vm = createViewModel()

        vm.initialize(wordId = 1, currentTagIds = emptyList())

        assertEquals(listOf(tagA, tagB), vm.currentState.tags)
    }

    @Test
    fun `initialize sets isLoading false after tags are loaded`() = runTest {
        tagsFlow.value = listOf(tagA)
        val vm = createViewModel()

        vm.initialize(wordId = 1, currentTagIds = emptyList())

        assertFalse(vm.currentState.isLoading)
    }

    @Test
    fun `initialize updates tags when flow emits new values`() = runTest {
        val vm = createViewModel()
        vm.initialize(wordId = 1, currentTagIds = emptyList())

        tagsFlow.value = listOf(tagA, tagC)

        assertEquals(listOf(tagA, tagC), vm.currentState.tags)
    }

    // --- toggleTag ---

    @Test
    fun `toggleTag adds tag when not currently selected`() = runTest {
        val vm = createViewModel()
        vm.initialize(wordId = 1, currentTagIds = emptyList())

        vm.toggleTag(tagId = 1L)

        assertTrue(vm.currentState.selectedTagIds.contains(1L))
    }

    @Test
    fun `toggleTag removes tag when already selected`() = runTest {
        val vm = createViewModel()
        vm.initialize(wordId = 1, currentTagIds = listOf(1L, 2L))

        vm.toggleTag(tagId = 1L)

        assertFalse(vm.currentState.selectedTagIds.contains(1L))
        assertTrue(vm.currentState.selectedTagIds.contains(2L))
    }

    @Test
    fun `toggleTag twice on same tag returns to original state`() = runTest {
        val vm = createViewModel()
        vm.initialize(wordId = 1, currentTagIds = emptyList())

        vm.toggleTag(tagId = 2L)
        vm.toggleTag(tagId = 2L)

        assertFalse(vm.currentState.selectedTagIds.contains(2L))
    }

    @Test
    fun `toggleTag can select multiple tags independently`() = runTest {
        val vm = createViewModel()
        vm.initialize(wordId = 1, currentTagIds = emptyList())

        vm.toggleTag(tagId = 1L)
        vm.toggleTag(tagId = 2L)
        vm.toggleTag(tagId = 3L)

        assertEquals(setOf(1L, 2L, 3L), vm.currentState.selectedTagIds)
    }

    // --- save ---

    @Test
    fun `save on success emits TagsAssigned effect`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = createViewModel()
            vm.initialize(wordId = 5, currentTagIds = listOf(1L))

            vm.save()

            val effect = vm.effects.first()
            assertIs<WordTagAssignmentEffect.TagsAssigned>(effect)
        }

    @Test
    fun `save on success isSaving is false after operation`() = runTest {
        val vm = createViewModel()
        vm.initialize(wordId = 5, currentTagIds = listOf(1L))

        vm.save()

        assertFalse(vm.currentState.isSaving)
    }

    @Test
    fun `save passes correct wordId and selectedTagIds to use case`() = runTest {
        val vm = createViewModel()
        vm.initialize(wordId = 10, currentTagIds = listOf(2L, 3L))

        vm.save()

        assertEquals(10L, capturedWordId)
        assertEquals(listOf(2L, 3L).toSet(), capturedTagIds?.toSet())
    }

    @Test
    fun `save passes empty selectedTagIds when none are selected`() = runTest {
        val vm = createViewModel()
        vm.initialize(wordId = 3, currentTagIds = emptyList())

        vm.save()

        assertEquals(3L, capturedWordId)
        assertEquals(emptyList(), capturedTagIds)
    }

    @Test
    fun `save on failure emits Error effect with message`() =
        runTest(UnconfinedTestDispatcher()) {
            assignResult = Try.failure(RuntimeException("Network error"))
            val vm = createViewModel()
            vm.initialize(wordId = 1, currentTagIds = emptyList())

            vm.save()

            val effect = vm.effects.first()
            assertIs<WordTagAssignmentEffect.Error>(effect)
            assertEquals("Network error", effect.message)
        }

    @Test
    fun `save on failure with no message uses fallback text`() =
        runTest(UnconfinedTestDispatcher()) {
            assignResult = Try.failure(RuntimeException())
            val vm = createViewModel()
            vm.initialize(wordId = 1, currentTagIds = emptyList())

            vm.save()

            val effect = vm.effects.first()
            assertIs<WordTagAssignmentEffect.Error>(effect)
            assertEquals("Failed to save tags", effect.message)
        }

    @Test
    fun `save on failure isSaving is false after operation`() = runTest {
        assignResult = Try.failure(RuntimeException("error"))
        val vm = createViewModel()
        vm.initialize(wordId = 1, currentTagIds = emptyList())

        vm.save()

        assertFalse(vm.currentState.isSaving)
    }

    @Test
    fun `save uses updated selectedTagIds after toggleTag`() = runTest {
        val vm = createViewModel()
        vm.initialize(wordId = 1, currentTagIds = listOf(1L, 2L))
        vm.toggleTag(tagId = 2L)
        vm.toggleTag(tagId = 3L)

        vm.save()

        assertEquals(setOf(1L, 3L), capturedTagIds?.toSet())
    }
}
