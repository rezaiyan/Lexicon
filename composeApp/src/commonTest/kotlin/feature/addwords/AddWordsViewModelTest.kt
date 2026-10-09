package feature.addwords

import app.cash.turbine.test
import core.common.Try
import core.common.getOrThrow
import domain.tag.usecase.CreateTagUseCase
import domain.tag.usecase.GetTagsUseCase
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft
import domain.word.add.model.WordOrigin
import domain.word.add.parser.RejectReason
import domain.word.add.parser.RejectedLine
import domain.word.add.usecase.AddWordsUseCase
import domain.word.add.usecase.ResolveAddWordsLanguagesUseCase
import core.error.DomainError
import fakes.FakeAddWordsLanguageRepository
import fakes.FakeAnalyticsTracker
import fakes.FakeCreditsRepository
import fakes.creditBalance
import domain.credits.usecase.ObserveCreditsUseCase
import domain.credits.usecase.RefreshCreditsUseCase
import fakes.FakeLearningFocusRepository
import fakes.FakeTagRepository
import fakes.FakeWordRepository
import feature.addwords.model.AddWordsEffect
import feature.addwords.model.AddWordsProblem
import feature.addwords.model.AddWordsResult
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
    private val credits = FakeCreditsRepository()

    private fun draft(term: String, translation: String = "t") = WordDraft.of(term, translation).getOrThrow()

    private fun createViewModel() = AddWordsViewModel(
        resolveLanguages = ResolveAddWordsLanguagesUseCase(languages, FakeLearningFocusRepository()),
        languageRepository = languages,
        addWords = AddWordsUseCase(words, Clock.System),
        getTags = GetTagsUseCase(tags),
        createTagUseCase = CreateTagUseCase(tags),
        observeCredits = ObserveCreditsUseCase(credits),
        refreshCredits = RefreshCreditsUseCase(credits),
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
    fun `init re-reads the credit balance from the server`() {
        val fresh = creditBalance(allowanceRemaining = 2)
        credits.serverBalance = fresh

        val vm = createViewModel()

        assertEquals(1, credits.refreshCount)
        assertEquals(fresh, vm.currentState.credits)
    }

    @Test
    fun `credits stay unknown when the balance can't be loaded`() {
        credits.failure = DomainError.Network.NoConnection

        assertNull(createViewModel().currentState.credits)
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
            listOf<Pair<String, Map<String, Any>?>>(
                "import_preview_shown" to mapOf("method" to "file", "word_count" to 1, "rejected_count" to 0),
                "import_failed" to mapOf("method" to "file", "step" to "commit", "error_type" to "Generic"),
            ),
            analytics.events,
        )
        assertEquals(emptyList<Pair<Int, String>>(), analytics.wordsImported)
    }

    @Test
    fun `the funnel logs start then preview then confirmation with the method`() {
        val vm = createViewModel()

        vm.sourceOpened(WordOrigin.Photo)
        vm.openReview(
            WordOrigin.Photo,
            listOf(draft("Hund", "dog"), draft("Maus", "mouse")),
            listOf(RejectedLine(3, "?", RejectReason.Malformed)),
        )
        vm.commitReview()

        assertEquals(
            listOf<Pair<String, Map<String, Any>?>>(
                "import_started" to mapOf("method" to "image"),
                "import_preview_shown" to mapOf("method" to "image", "word_count" to 2, "rejected_count" to 1),
                "import_confirmed" to mapOf("method" to "image", "word_count" to 2, "duplicates" to 0),
            ),
            analytics.events,
        )
    }

    @Test
    fun `discarding the review logs a cancel at the review step`() {
        val vm = createViewModel()
        vm.openReview(WordOrigin.AiSuggestion, listOf(draft("Hund", "dog")))

        vm.discardReview()

        assertEquals(
            "import_cancelled" to mapOf<String, Any>("method" to "ai", "at_step" to "review"),
            analytics.events.last(),
        )
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
    fun `balance shown follows every change to the shared balance without the screen asking`() {
        credits.serverBalance = creditBalance()
        val vm = createViewModel()

        credits.balance.value = creditBalance(allowanceRemaining = 2) // e.g. a spend elsewhere

        assertEquals(17, vm.currentState.credits?.balance)
        assertEquals(1, credits.refreshCount) // only the read on open
    }
}
