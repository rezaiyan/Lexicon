package presentation.feature.aiimport

import core.common.Try
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.model.SuggestedVocabularyResponse
import domain.onboarding.repository.IOnboardingRepository
import domain.onboarding.usecase.ImportSuggestedVocabularyUseCase
import domain.onboarding.usecase.SubmitPreferencesUseCase
import domain.tag.model.Tag
import domain.tag.repository.ITagRepository
import domain.tag.usecase.GetTagsUseCase
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import fakes.FakeAnalyticsTracker
import feature.aiimport.AiWordImportViewModel
import feature.aiimport.model.AiWordImportEffect
import feature.aiimport.model.AiWordImportStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AiWordImportViewModelTest : ViewModelTestBase() {

    private var submitResult: Try<SuggestedVocabularyResponse> = Try.success(
        SuggestedVocabularyResponse(
            suggestedVocabulary = listOf(
                SuggestedVocabulary("hola", "hello", "greeting", "es", "en"),
                SuggestedVocabulary("gato", "cat", "animal", "es", "en")
            ),
            targetLanguage = "Spanish",
            nativeLanguage = "English",
            currentLevel = "beginner"
        )
    )
    private var importResult: Try<Int> = Try.success(2)

    private fun fakeOnboardingRepo() = object : IOnboardingRepository {
        override suspend fun submitPreferences(preferences: OnboardingPreferences): Try<SuggestedVocabularyResponse> = submitResult
        override suspend fun hasCompletedOnboarding(): Try<Boolean> = Try.success(true)
        override suspend fun markOnboardingCompleted(): Try<Unit> = Try.success(Unit)
    }

    private fun fakeWordRepo() = object : IWordRepository {
        override suspend fun insertWords(words: List<Word>): Try<Int> = importResult
        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCardsByTag(tagId: Long): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf()
        override fun updateWordsLanguages(ids: List<Int>, sourceLanguage: String, targetLanguage: String): Flow<UpdateWordsLanguagesProgress> = flowOf()
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = flowOf()
        override suspend fun getTotalCount(): Try<Int> = Try.success(0)
        override suspend fun getDueCount(): Try<Int> = Try.success(0)
        override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
    }

    private fun fakeTagRepo() = object : ITagRepository {
        override fun getTags(): Flow<List<Tag>> = flowOf(emptyList())
        override fun getTagsByLevel(): Flow<Map<Int, List<Tag>>> = flowOf(emptyMap())
        override fun getDueTags(): Flow<List<Tag>> = flowOf(emptyList())
        override suspend fun createTag(name: String): Try<Tag> = Try.success(Tag(1L, name, 0L, 0L, 0L))
        override suspend fun renameTag(id: Long, name: String): Try<Tag> = Try.success(Tag(id, name, 0L, 0L, 0L))
        override suspend fun deleteTag(id: Long): Try<Unit> = Try.success(Unit)
        override suspend fun assignWordTags(wordId: Long, tagIds: List<Long>): Try<Unit> = Try.success(Unit)
        override suspend fun batchAssignWordTags(wordIds: List<Long>, tagIds: List<Long>): Try<Unit> = Try.success(Unit)
        override suspend fun syncTagsFromRemote(): Try<Unit> = Try.success(Unit)
    }

    private fun createViewModel() = AiWordImportViewModel(
        submitPreferencesUseCase = SubmitPreferencesUseCase(fakeOnboardingRepo()),
        importSuggestedVocabularyUseCase = ImportSuggestedVocabularyUseCase(fakeWordRepo()),
        getTagsUseCase = GetTagsUseCase(fakeTagRepo()),
        analyticsTracker = FakeAnalyticsTracker(),
    )

    @Test
    fun `initial state starts at TARGET_LANG step`() {
        val vm = createViewModel()
        assertEquals(AiWordImportStep.TARGET_LANG, vm.currentState.step)
        assertEquals(false, vm.currentState.isLoading)
    }

    @Test
    fun `nextStep advances through steps`() {
        val vm = createViewModel()
        assertEquals(AiWordImportStep.TARGET_LANG, vm.currentState.step)
        vm.nextStep()
        assertEquals(AiWordImportStep.NATIVE_LANG, vm.currentState.step)
        vm.nextStep()
        assertEquals(AiWordImportStep.LEVEL, vm.currentState.step)
        vm.nextStep()
        assertEquals(AiWordImportStep.TOPICS, vm.currentState.step)
    }

    @Test
    fun `previousStep goes back`() {
        val vm = createViewModel()
        vm.nextStep()
        vm.nextStep()
        assertEquals(AiWordImportStep.LEVEL, vm.currentState.step)
        vm.previousStep()
        assertEquals(AiWordImportStep.NATIVE_LANG, vm.currentState.step)
    }

    @Test
    fun `toggleTopic adds and removes topics`() {
        val vm = createViewModel()
        vm.toggleTopic("Travel")
        assertEquals(setOf("Travel"), vm.currentState.selectedTopics)
        vm.toggleTopic("Food")
        assertEquals(setOf("Travel", "Food"), vm.currentState.selectedTopics)
        vm.toggleTopic("Travel")
        assertEquals(setOf("Food"), vm.currentState.selectedTopics)
    }

    @Test
    fun `submit with valid preferences moves to PREVIEW`() = runTest {
        val vm = createViewModel()
        vm.selectTargetLanguage("Spanish")
        vm.selectNativeLanguage("English")
        vm.selectLevel("beginner")

        vm.submit()

        assertEquals(AiWordImportStep.PREVIEW, vm.currentState.step)
        assertEquals(2, vm.currentState.suggestedWords.size)
        assertEquals(setOf(0, 1), vm.currentState.selectedWordIndices)
        assertEquals(false, vm.currentState.isLoading)
    }

    @Test
    fun `submit with missing selections does not advance`() = runTest {
        val vm = createViewModel()
        vm.selectTargetLanguage("Spanish")
        // Missing native language and level

        vm.submit()

        // selectTargetLanguage auto-advances to NATIVE_LANG; submit early-returns
        assertEquals(AiWordImportStep.NATIVE_LANG, vm.currentState.step)
    }

    @Test
    fun `submit failure sets error`() = runTest {
        submitResult = Try.failure(RuntimeException("API error"))
        val vm = createViewModel()
        vm.selectTargetLanguage("Spanish")
        vm.selectNativeLanguage("English")
        vm.selectLevel("beginner")

        vm.submit()

        assertEquals("API error", vm.currentState.error)
        assertEquals(false, vm.currentState.isLoading)
    }

    @Test
    fun `toggleWordSelection adds and removes indices`() = runTest {
        val vm = createViewModel()
        vm.selectTargetLanguage("Spanish")
        vm.selectNativeLanguage("English")
        vm.selectLevel("beginner")
        vm.submit()

        // Initially all selected
        assertEquals(setOf(0, 1), vm.currentState.selectedWordIndices)
        vm.toggleWordSelection(0)
        assertEquals(setOf(1), vm.currentState.selectedWordIndices)
        vm.toggleWordSelection(0)
        assertEquals(setOf(0, 1), vm.currentState.selectedWordIndices)
    }

    @Test
    fun `importSelected emits ImportSuccess`() = runTest(UnconfinedTestDispatcher()) {
        val vm = createViewModel()
        vm.selectTargetLanguage("Spanish")
        vm.selectNativeLanguage("English")
        vm.selectLevel("beginner")
        vm.submit()

        vm.importSelected()

        val event = vm.effects.first()
        assertIs<AiWordImportEffect.ImportSuccess>(event)
        assertEquals(2, event.count)
    }

    @Test
    fun `dismiss emits Dismiss event`() = runTest(UnconfinedTestDispatcher()) {
        val vm = createViewModel()

        vm.dismiss()

        val event = vm.effects.first()
        assertEquals(AiWordImportEffect.Dismiss, event)
    }

    @Test
    fun `reset returns to initial state`() = runTest {
        val vm = createViewModel()
        vm.selectTargetLanguage("Spanish")
        vm.nextStep()

        vm.reset()

        assertEquals(AiWordImportStep.TARGET_LANG, vm.currentState.step)
        assertEquals(null, vm.currentState.selectedTargetLanguage)
    }
}
