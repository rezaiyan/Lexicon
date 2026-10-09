package feature.addwords

import app.cash.turbine.test
import core.common.Try
import core.common.getOrThrow
import domain.auth.model.AuthUser
import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.UserFeatureAccess
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.tag.usecase.CreateTagUseCase
import domain.tag.usecase.GetTagsUseCase
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft
import domain.word.add.model.WordOrigin
import domain.word.add.usecase.AddWordsUseCase
import domain.word.add.usecase.ResolveAddWordsLanguagesUseCase
import domain.word.usecase.ObserveImageImportAccessUseCase
import core.error.DomainError
import fakes.FakeAddWordsLanguageRepository
import fakes.FakeAnalyticsTracker
import fakes.FakeAuthRepository
import fakes.FakeLearningFocusRepository
import fakes.FakeSubscriptionManager
import fakes.FakeTagRepository
import fakes.FakeUserManager
import fakes.FakeWordRepository
import feature.addwords.model.AddWordsEffect
import feature.addwords.model.AddWordsProblem
import feature.addwords.model.AddWordsResult
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

class AddWordsViewModelTest : ViewModelTestBase() {

    private val germanFromEnglish = LanguagePair(Language.GERMAN, Language.ENGLISH)
    private val words = FakeWordRepository()
    private val languages = FakeAddWordsLanguageRepository(lastUsed = germanFromEnglish)
    private val tags = FakeTagRepository()
    private val analytics = FakeAnalyticsTracker()
    private val auth = FakeAuthRepository()
    private val userManager = FakeUserManager()

    private fun draft(term: String, translation: String = "t") = WordDraft.of(term, translation).getOrThrow()

    private fun createViewModel() = AddWordsViewModel(
        resolveLanguages = ResolveAddWordsLanguagesUseCase(languages, FakeLearningFocusRepository()),
        languageRepository = languages,
        addWords = AddWordsUseCase(words, Clock.System),
        getTags = GetTagsUseCase(tags),
        createTagUseCase = CreateTagUseCase(tags),
        observePremiumTools = ObserveImageImportAccessUseCase(
            userManager,
            GetFeatureAccessUseCase(auth, FakeSubscriptionManager()),
        ),
        analytics = analytics,
    )

    @Test
    fun `init resolves the language pair`() {
        val vm = createViewModel()

        assertEquals(germanFromEnglish, vm.currentState.languages)
        assertTrue(vm.currentState.languagesLoaded)
    }

    @Test
    fun `init without history leaves languages unset`() {
        languages.lastUsed = null

        val vm = createViewModel()

        assertNull(vm.currentState.languages)
        assertTrue(vm.currentState.languagesLoaded)
    }

    @Test
    fun `init reports premium tools for a premium user`() {
        userManager.userFlow.value = AuthUser(1L, "a@b.c", "A")
        auth.featureAccessFlow = flowOf(FeatureAccessResponse(FeatureFlags(), UserFeatureAccess(hasPremiumAccess = true)))

        assertTrue(createViewModel().currentState.hasPremiumTools)
    }

    @Test
    fun `setLearningLanguage equal to native clears native`() {
        val vm = createViewModel()

        vm.setLearningLanguage(Language.ENGLISH)

        assertEquals(Language.ENGLISH, vm.currentState.learning)
        assertNull(vm.currentState.native)
        assertNull(vm.currentState.languages)
    }

    @Test
    fun `swapLanguages swaps learning and native`() {
        val vm = createViewModel()

        vm.swapLanguages()

        assertEquals(LanguagePair(Language.ENGLISH, Language.GERMAN), vm.currentState.languages)
    }

    @Test
    fun `createTag selects the new tag`() {
        tags.nextTagId = 7L
        val vm = createViewModel()

        vm.createTag("Travel")

        assertEquals(7L, vm.currentState.selectedTagId)
    }

    @Test
    fun `createTag failure shows a problem and keeps the selection`() {
        tags.createTagResult = Try.failure(DomainError.Network.ServerError(500))
        val vm = createViewModel()

        vm.createTag("Travel")

        assertNull(vm.currentState.selectedTagId)
        assertEquals(AddWordsProblem.Generic, vm.currentState.problem)
    }

    @Test
    fun `openReview shows the candidates and asks to open the review page`() = runTest {
        val vm = createViewModel()

        vm.effects.test {
            vm.openReview(WordOrigin.File, listOf(draft("Hund"), draft("Katze")))

            assertEquals(AddWordsEffect.OpenReview, awaitItem())
        }
        assertEquals(2, vm.currentState.review?.selectedCount)
    }

    @Test
    fun `commitReview adds only the selected words with the sheet context`() = runTest {
        val vm = createViewModel()
        vm.selectTag(3L)
        vm.openReview(WordOrigin.Photo, listOf(draft("Hund", "dog"), draft("Katze", "cat")))
        vm.toggleCandidate(1)

        vm.commitReview()

        val added = words.addedWords.single()
        assertEquals("Hund", added.originalWord)
        assertEquals(Language.GERMAN, added.targetLanguage)
        assertEquals(Language.ENGLISH, added.sourceLanguage)
        assertEquals(listOf(3L), added.tagIds)
    }

    @Test
    fun `commitReview success shows the result and saves the pair and logs the origin`() = runTest {
        languages.lastUsed = null
        val vm = createViewModel()
        vm.setLearningLanguage(Language.FRENCH)
        vm.setNativeLanguage(Language.ENGLISH)
        vm.openReview(WordOrigin.AiSuggestion, listOf(draft("chien", "dog")))

        vm.effects.test {
            skipItems(1) // OpenReview
            vm.commitReview()
            assertEquals(AddWordsEffect.ShowResult, awaitItem())
        }

        assertEquals(AddWordsResult(WordOrigin.AiSuggestion, 1, 0, listOf("chien")), vm.currentState.result)
        assertNull(vm.currentState.review)
        assertEquals(LanguagePair(Language.FRENCH, Language.ENGLISH), languages.lastUsed)
        assertEquals(listOf(1 to "ai"), analytics.wordsImported)
    }

    @Test
    fun `commitReview counts words the user already has as duplicates`() {
        val vm = createViewModel()
        vm.openReview(WordOrigin.File, listOf(draft("Hund", "dog")))
        vm.commitReview()
        vm.openReview(WordOrigin.File, listOf(draft("hund", "Dog"), draft("Maus", "mouse")))

        vm.commitReview()

        assertEquals(1, vm.currentState.result?.added)
        assertEquals(1, vm.currentState.result?.duplicates)
        assertEquals(listOf("Maus"), vm.currentState.result?.previewTerms)
    }

    @Test
    fun `commitReview failure keeps the review and shows the problem`() {
        words.addResult = Try.failure(DomainError.Network.ServerError(500))
        val vm = createViewModel()
        vm.openReview(WordOrigin.File, listOf(draft("Hund")))

        vm.commitReview()

        assertNotNull(vm.currentState.review)
        assertFalse(vm.currentState.isCommitting)
        assertEquals(AddWordsProblem.Generic, vm.currentState.problem)
        assertNull(vm.currentState.result)
        assertEquals(
            listOf<Pair<String, Map<String, Any>?>>("import_failed" to mapOf("method" to "file", "reason" to "Generic")),
            analytics.events,
        )
        assertEquals(emptyList<Pair<Int, String>>(), analytics.wordsImported)
    }

    @Test
    fun `commitReview when every word is a duplicate logs no import`() {
        val vm = createViewModel()
        vm.openReview(WordOrigin.File, listOf(draft("Hund", "dog")))
        vm.commitReview()
        vm.openReview(WordOrigin.File, listOf(draft("hund", "Dog")))

        vm.commitReview()

        assertEquals(0, vm.currentState.result?.added)
        assertEquals(listOf(1 to "file"), analytics.wordsImported)
    }

    @Test
    fun `commitReview without a language pair does nothing`() {
        languages.lastUsed = null
        val vm = createViewModel()
        vm.openReview(WordOrigin.File, listOf(draft("Hund")))

        vm.commitReview()

        assertTrue(words.addedWords.isEmpty())
        assertNotNull(vm.currentState.review)
    }

    @Test
    fun `commitReview with nothing selected does nothing`() {
        val vm = createViewModel()
        vm.openReview(WordOrigin.File, listOf(draft("Hund")))
        vm.setAllCandidatesSelected(false)

        vm.commitReview()

        assertTrue(words.addedWords.isEmpty())
    }

    @Test
    fun `saveCandidateEdit with a blank translation keeps editing and shows the problem`() {
        val vm = createViewModel()
        vm.openReview(WordOrigin.Photo, listOf(draft("Hund")))
        vm.startEditingCandidate(0)

        vm.saveCandidateEdit("Hund", "", "")

        assertEquals(0, vm.currentState.review?.editingId)
        assertEquals(AddWordsProblem.MissingTranslation, vm.currentState.problem)
    }

    @Test
    fun `finishManualEntry shows the result and saves the pair`() = runTest {
        languages.lastUsed = null
        val vm = createViewModel()
        vm.setLearningLanguage(Language.SPANISH)
        vm.setNativeLanguage(Language.ENGLISH)

        vm.finishManualEntry(added = 2, recentTerms = listOf("perro", "gato"))

        assertEquals(AddWordsResult(WordOrigin.Manual, 2, 0, listOf("perro", "gato")), vm.currentState.result)
        assertEquals(LanguagePair(Language.SPANISH, Language.ENGLISH), languages.lastUsed)
    }

    @Test
    fun `onPremiumLapsed hides premium tools`() {
        userManager.userFlow.value = AuthUser(1L, "a@b.c", "A")
        auth.featureAccessFlow = flowOf(FeatureAccessResponse(FeatureFlags(), UserFeatureAccess(hasPremiumAccess = true)))
        val vm = createViewModel()

        vm.onPremiumLapsed()

        assertFalse(vm.currentState.hasPremiumTools)
    }
}
