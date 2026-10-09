package feature.addwords

import app.cash.turbine.test
import core.common.Try
import core.error.DomainError
import domain.word.add.model.LanguagePair
import domain.word.add.usecase.AddWordsUseCase
import fakes.FakeAnalyticsTracker
import fakes.FakeWordRepository
import feature.addwords.model.AddWordsProblem
import feature.addwords.source.AddedWord
import feature.addwords.source.ManualEntryEffect
import feature.addwords.source.ManualEntryViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

class ManualEntryViewModelTest : ViewModelTestBase() {

    private val languages = LanguagePair(Language.GERMAN, Language.ENGLISH)
    private val words = FakeWordRepository()
    private val analytics = FakeAnalyticsTracker()

    private fun createViewModel() = ManualEntryViewModel(AddWordsUseCase(words, Clock.System), analytics)

    private fun ManualEntryViewModel.type(term: String, translation: String, note: String = "") {
        setTerm(term)
        setTranslation(translation)
        setNote(note)
    }

    @Test
    fun `add saves the word and clears the form`() = runTest {
        val vm = createViewModel()
        vm.type(" Hund ", "dog", "pet")

        vm.effects.test {
            vm.add(languages, setOf(4L))
            assertEquals(ManualEntryEffect.WordAdded, awaitItem())
        }

        val saved = words.addedWords.single()
        assertEquals("Hund", saved.originalWord)
        assertEquals("pet", saved.description)
        assertEquals(listOf(4L), saved.tagIds)
        assertEquals("", vm.currentState.term)
        assertEquals(1, vm.currentState.addedCount)
        assertEquals(listOf(AddedWord("Hund", "dog")), vm.currentState.recent)
        assertEquals(listOf(1 to "manual"), analytics.wordsImported)
    }

    @Test
    fun `add keeps the newest words first`() {
        val vm = createViewModel()
        vm.type("Hund", "dog")
        vm.add(languages, emptySet())
        vm.type("Katze", "cat")

        vm.add(languages, emptySet())

        assertEquals(listOf("Katze", "Hund"), vm.currentState.recent.map { it.term })
        assertEquals(2, vm.currentState.addedCount)
    }

    @Test
    fun `add of a word the user already has keeps the input and says so`() {
        val vm = createViewModel()
        vm.type("Hund", "dog")
        vm.add(languages, emptySet())
        vm.type("hund", "Dog")

        vm.add(languages, emptySet())

        assertTrue(vm.currentState.alreadyAdded)
        assertEquals("hund", vm.currentState.term)
        assertEquals(1, vm.currentState.addedCount)
        assertEquals(1, words.addedWords.size)
    }

    @Test
    fun `add without a translation does nothing`() {
        val vm = createViewModel()
        vm.type("Hund", " ")

        vm.add(languages, emptySet())

        assertFalse(vm.currentState.canAdd)
        assertTrue(words.addedWords.isEmpty())
    }

    @Test
    fun `add without a language pair does nothing`() {
        val vm = createViewModel()
        vm.type("Hund", "dog")

        vm.add(null, emptySet())

        assertTrue(words.addedWords.isEmpty())
    }

    @Test
    fun `add of a too long word shows the problem`() {
        val vm = createViewModel()
        vm.type("x".repeat(500), "dog")

        vm.add(languages, emptySet())

        assertEquals(AddWordsProblem.TooLong, vm.currentState.problem)
        assertFalse(vm.currentState.isSaving)
    }

    @Test
    fun `add failure keeps the input and shows the problem`() {
        words.addResult = Try.failure(DomainError.Network.ServerError(500))
        val vm = createViewModel()
        vm.type("Hund", "dog")

        vm.add(languages, emptySet())

        assertEquals(AddWordsProblem.Generic, vm.currentState.problem)
        assertEquals("Hund", vm.currentState.term)
    }

    @Test
    fun `add while saving is ignored`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val slowWords = object : domain.word.repository.IWordRepository by words {
            override suspend fun addWords(words: List<domain.word.model.Word>) =
                gate.await().let { this@ManualEntryViewModelTest.words.addWords(words) }
        }
        val vm = ManualEntryViewModel(AddWordsUseCase(slowWords, Clock.System), analytics)
        vm.type("Hund", "dog")

        vm.add(languages, emptySet())
        vm.add(languages, emptySet())
        gate.complete(Unit)

        assertEquals(1, words.addedWords.size)
    }

    @Test
    fun `clearSession starts a new batch`() {
        val vm = createViewModel()
        vm.type("Hund", "dog")
        vm.add(languages, emptySet())

        vm.clearSession()

        assertEquals(0, vm.currentState.addedCount)
        assertTrue(vm.currentState.recent.isEmpty())
    }
}
