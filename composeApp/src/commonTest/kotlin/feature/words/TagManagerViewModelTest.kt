package feature.words

import core.common.Try
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.GetSkipTagSelectorUseCase
import domain.settings.usecase.SetSkipTagSelectorUseCase
import domain.tag.model.Tag
import domain.tag.repository.ITagRepository
import domain.tag.usecase.CreateTagUseCase
import domain.tag.usecase.DeleteTagUseCase
import domain.tag.usecase.GetTagsUseCase
import domain.tag.usecase.RenameTagUseCase
import feature.words.model.TagManagerEffect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import utils.Language
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class TagManagerViewModelTest : ViewModelTestBase() {

    private val tagA = Tag(id = 1L, name = "Spanish", wordCount = 5L, createdAt = 0L, updatedAt = 0L)
    private val tagB = Tag(id = 2L, name = "French", wordCount = 3L, createdAt = 0L, updatedAt = 0L)

    private val tagsFlow = MutableStateFlow<List<Tag>>(emptyList())
    private var createResult: Try<Tag> = Try.success(tagA)
    private var renameResult: Try<Tag> = Try.success(tagA)
    private var deleteResult: Try<Unit> = Try.success(Unit)

    private fun fakeRepo() = object : ITagRepository {
        override fun getTags(): Flow<List<Tag>> = tagsFlow
        override fun getTagsByLevel(): Flow<Map<Int, List<Tag>>> = flowOf(emptyMap())
        override fun getDueTags(): Flow<List<Tag>> = flowOf(emptyList())
        override suspend fun createTag(name: String): Try<Tag> = createResult
        override suspend fun renameTag(id: Long, name: String): Try<Tag> = renameResult
        override suspend fun deleteTag(id: Long): Try<Unit> = deleteResult
        override suspend fun assignWordTags(wordId: Long, tagIds: List<Long>): Try<Unit> = Try.success(Unit)
        override suspend fun batchAssignWordTags(wordIds: List<Long>, tagIds: List<Long>): Try<Unit> = Try.success(Unit)
        override suspend fun syncTagsFromRemote(): Try<Unit> = Try.success(Unit)
    }

    private fun fakeSettingsRepo() = object : ISettingsRepository {
        override fun getLanguage(): Flow<Language> = flowOf(Language.ENGLISH)
        override suspend fun setLanguage(language: Language): Try<Unit> = Try.success(Unit)
        override fun getThemeMode(): Flow<ThemeMode> = flowOf(ThemeMode.AUTO)
        override suspend fun setThemeMode(mode: ThemeMode): Try<Unit> = Try.success(Unit)
        override suspend fun clearSettings(): Try<Unit> = Try.success(Unit)
        override fun getNotificationsEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setNotificationsEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setReviewRemindersEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setMotivationalMessagesEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override suspend fun getDailyReminderTime(): Try<String> = Try.success("09:00")
        override suspend fun setDailyReminderTime(time: String): Try<Unit> = Try.success(Unit)
        override suspend fun getMinimumDueCards(): Try<Int> = Try.success(5)
        override suspend fun setMinimumDueCards(count: Int): Try<Unit> = Try.success(Unit)
    }

    private fun createViewModel() = TagManagerViewModel(
        getTagsUseCase = GetTagsUseCase(fakeRepo()),
        createTagUseCase = CreateTagUseCase(fakeRepo()),
        renameTagUseCase = RenameTagUseCase(fakeRepo()),
        deleteTagUseCase = DeleteTagUseCase(fakeRepo()),
        setSkipTagSelectorUseCase = SetSkipTagSelectorUseCase(fakeSettingsRepo()),
        getSkipTagSelectorUseCase = GetSkipTagSelectorUseCase(fakeSettingsRepo()),
    )

    // --- init / tag loading ---

    @Test
    fun `init loads tags from repository into state`() = runTest {
        tagsFlow.value = listOf(tagA, tagB)

        val vm = createViewModel()

        assertEquals(listOf(tagA, tagB), vm.currentState.tags)
        assertFalse(vm.currentState.isLoading)
    }

    @Test
    fun `init updates state when tags flow emits new values`() = runTest {
        val vm = createViewModel()
        assertEquals(emptyList(), vm.currentState.tags)

        tagsFlow.value = listOf(tagA)

        assertEquals(listOf(tagA), vm.currentState.tags)
    }

    @Test
    fun `init clears errorMessage when tags load successfully`() = runTest {
        tagsFlow.value = listOf(tagA)

        val vm = createViewModel()

        assertNull(vm.currentState.errorMessage)
    }

    // --- createTag ---

    @Test
    fun `createTag on success emits TagCreated effect with correct tag`() =
        runTest(UnconfinedTestDispatcher()) {
            createResult = Try.success(tagA)
            val vm = createViewModel()

            vm.createTag("Spanish")

            val effect = vm.effects.first()
            assertIs<TagManagerEffect.TagCreated>(effect)
            assertEquals(tagA, effect.tag)
        }

    @Test
    fun `createTag on success isLoading is false after operation`() = runTest {
        createResult = Try.success(tagA)
        val vm = createViewModel()

        vm.createTag("Spanish")

        assertFalse(vm.currentState.isLoading)
    }

    @Test
    fun `createTag on failure emits Error effect with message`() =
        runTest(UnconfinedTestDispatcher()) {
            createResult = Try.failure(RuntimeException("Name already exists"))
            val vm = createViewModel()

            vm.createTag("Spanish")

            val effect = vm.effects.first()
            assertIs<TagManagerEffect.Error>(effect)
            assertEquals("Name already exists", effect.message)
        }

    @Test
    fun `createTag on failure with no message uses fallback text`() =
        runTest(UnconfinedTestDispatcher()) {
            createResult = Try.failure(RuntimeException())
            val vm = createViewModel()

            vm.createTag("Spanish")

            val effect = vm.effects.first()
            assertIs<TagManagerEffect.Error>(effect)
            assertEquals("Something went wrong. Please try again.", effect.message)
        }

    @Test
    fun `createTag on failure isLoading is false after operation`() = runTest {
        createResult = Try.failure(RuntimeException("error"))
        val vm = createViewModel()

        vm.createTag("Spanish")

        assertFalse(vm.currentState.isLoading)
    }

    // --- renameTag ---

    @Test
    fun `renameTag on success emits TagRenamed effect with updated tag`() =
        runTest(UnconfinedTestDispatcher()) {
            val renamed = tagA.copy(name = "Espanol")
            renameResult = Try.success(renamed)
            val vm = createViewModel()

            vm.renameTag(id = 1L, name = "Espanol")

            val effect = vm.effects.first()
            assertIs<TagManagerEffect.TagRenamed>(effect)
            assertEquals(renamed, effect.tag)
        }

    @Test
    fun `renameTag on failure emits Error effect with message`() =
        runTest(UnconfinedTestDispatcher()) {
            renameResult = Try.failure(RuntimeException("Tag not found"))
            val vm = createViewModel()

            vm.renameTag(id = 1L, name = "NewName")

            val effect = vm.effects.first()
            assertIs<TagManagerEffect.Error>(effect)
            assertEquals("Tag not found", effect.message)
        }

    @Test
    fun `renameTag on failure with no message uses fallback text`() =
        runTest(UnconfinedTestDispatcher()) {
            renameResult = Try.failure(RuntimeException())
            val vm = createViewModel()

            vm.renameTag(id = 1L, name = "NewName")

            val effect = vm.effects.first()
            assertIs<TagManagerEffect.Error>(effect)
            assertEquals("Something went wrong. Please try again.", effect.message)
        }

    // --- deleteTag ---

    @Test
    fun `deleteTag on success emits TagDeleted effect with correct id`() =
        runTest(UnconfinedTestDispatcher()) {
            deleteResult = Try.success(Unit)
            val vm = createViewModel()

            vm.deleteTag(id = 2L)

            val effect = vm.effects.first()
            assertIs<TagManagerEffect.TagDeleted>(effect)
            assertEquals(2L, effect.tagId)
        }

    @Test
    fun `deleteTag on success isLoading is false after operation`() = runTest {
        deleteResult = Try.success(Unit)
        val vm = createViewModel()

        vm.deleteTag(id = 1L)

        assertFalse(vm.currentState.isLoading)
    }

    @Test
    fun `deleteTag on failure emits Error effect with message`() =
        runTest(UnconfinedTestDispatcher()) {
            deleteResult = Try.failure(RuntimeException("Cannot delete tag"))
            val vm = createViewModel()

            vm.deleteTag(id = 1L)

            val effect = vm.effects.first()
            assertIs<TagManagerEffect.Error>(effect)
            assertEquals("Cannot delete tag", effect.message)
        }

    @Test
    fun `deleteTag on failure with no message uses fallback text`() =
        runTest(UnconfinedTestDispatcher()) {
            deleteResult = Try.failure(RuntimeException())
            val vm = createViewModel()

            vm.deleteTag(id = 1L)

            val effect = vm.effects.first()
            assertIs<TagManagerEffect.Error>(effect)
            assertEquals("Something went wrong. Please try again.", effect.message)
        }

    @Test
    fun `deleteTag on failure isLoading is false after operation`() = runTest {
        deleteResult = Try.failure(RuntimeException("error"))
        val vm = createViewModel()

        vm.deleteTag(id = 1L)

        assertFalse(vm.currentState.isLoading)
    }
}
