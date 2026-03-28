package feature.study.wordrush

import domain.word.model.Word
import domain.word.usecase.GetWordRushWordsUseCase
import fakes.FakeWordRepository
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WordRushViewModelTest : ViewModelTestBase() {

    private fun createWords(count: Int): List<Word> = (1..count).map { i ->
        Word(
            id = i,
            originalWord = "word_$i",
            translation = "translation_$i",
            description = "desc_$i",
            sourceLanguage = utils.Language.ENGLISH,
            targetLanguage = utils.Language.GERMAN,
            nextReviewDate = 0L,
        )
    }

    private fun fakeRepo(words: List<Word> = createWords(10)): FakeWordRepository {
        return FakeWordRepository().apply {
            storedWords = words.toMutableList()
        }
    }

    private fun createViewModel(words: List<Word> = createWords(10)): WordRushViewModel {
        val repo = fakeRepo(words)
        val useCase = GetWordRushWordsUseCase(repo)
        return WordRushViewModel(useCase)
    }

    @Test
    fun `initial phase is Idle`() {
        val vm = createViewModel()
        assertIs<WordRushPhase.Idle>(vm.currentState.phase)
    }

    @Test
    fun `hasEnoughWords is true when vocabulary has 4+ words`() {
        val vm = createViewModel(createWords(5))
        assertTrue(vm.currentState.hasEnoughWords)
    }

    @Test
    fun `hasEnoughWords is false when vocabulary has fewer than 4 words`() {
        val vm = createViewModel(createWords(2))
        assertEquals(false, vm.currentState.hasEnoughWords)
    }

    @Test
    fun `startGame transitions to Playing phase`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        assertIs<WordRushPhase.Playing>(vm.currentState.phase)
    }

    @Test
    fun `playing phase has correct initial values`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        val phase = vm.currentState.phase as WordRushPhase.Playing
        assertEquals(0, phase.questionIndex)
        assertEquals(0, phase.score)
        assertEquals(0, phase.streak)
        assertNull(phase.selectedIndex)
        assertNull(phase.isCorrect)
        assertEquals(4, phase.question.options.size)
    }

    @Test
    fun `question has correct answer in options`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        val phase = vm.currentState.phase as WordRushPhase.Playing
        val correctTranslation = phase.question.word.translation
        assertTrue(phase.question.options.contains(correctTranslation))
        assertEquals(correctTranslation, phase.question.options[phase.question.correctIndex])
    }

    @Test
    fun `selecting correct answer increases score and streak`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        val phase = vm.currentState.phase as WordRushPhase.Playing
        vm.selectAnswer(phase.question.correctIndex)
        val updated = vm.currentState.phase as WordRushPhase.Playing
        assertEquals(1, updated.score)
        assertEquals(1, updated.streak)
        assertTrue(updated.isCorrect == true)
    }

    @Test
    fun `selecting wrong answer resets streak`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        val phase = vm.currentState.phase as WordRushPhase.Playing
        val wrongIndex = (0..3).first { it != phase.question.correctIndex }
        vm.selectAnswer(wrongIndex)
        val updated = vm.currentState.phase as WordRushPhase.Playing
        assertEquals(0, updated.score)
        assertEquals(0, updated.streak)
        assertEquals(false, updated.isCorrect)
    }

    @Test
    fun `dismiss resets to Idle`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        vm.dismiss()
        assertIs<WordRushPhase.Idle>(vm.currentState.phase)
    }

    @Test
    fun `startGame with insufficient words shows Error`() = runTest {
        val vm = createViewModel(createWords(2))
        vm.startGame()
        assertIs<WordRushPhase.Error>(vm.currentState.phase)
    }

    @Test
    fun `initial bestStreak is 0`() {
        val vm = createViewModel()
        assertEquals(0, vm.currentState.bestStreak)
    }

    @Test
    fun `double selection is ignored`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        val phase = vm.currentState.phase as WordRushPhase.Playing
        val correctIndex = phase.question.correctIndex
        vm.selectAnswer(correctIndex)
        val stateAfterFirst = vm.currentState.phase
        // Second selection should be ignored
        val wrongIndex = (0..3).first { it != correctIndex }
        vm.selectAnswer(wrongIndex)
        assertEquals(stateAfterFirst, vm.currentState.phase)
    }
}
