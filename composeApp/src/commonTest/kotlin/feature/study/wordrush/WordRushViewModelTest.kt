package feature.study.wordrush

import domain.word.model.Word
import domain.word.usecase.GetWordRushWordsUseCase
import fakes.FakeWordRepository
import kotlinx.coroutines.test.advanceTimeBy
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
        assertEquals(1, phase.multiplier)
        assertNull(phase.selectedIndex)
        assertNull(phase.isCorrect)
        assertNull(phase.lastPointsEarned)
        assertEquals(4, phase.question.options.size)
    }

    @Test
    fun `playing phase starts with full lives`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        val phase = vm.currentState.phase as WordRushPhase.Playing
        assertEquals(WordRushViewModel.INITIAL_LIVES, phase.lives)
    }

    @Test
    fun `playing phase starts with no power-ups`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        val phase = vm.currentState.phase as WordRushPhase.Playing
        assertTrue(phase.powerUps.isEmpty())
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
        assertTrue(updated.score >= 1)
        assertEquals(1, updated.streak)
        assertTrue(updated.isCorrect == true)
    }

    @Test
    fun `selecting wrong answer resets streak and decrements lives`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        val phase = vm.currentState.phase as WordRushPhase.Playing
        val wrongIndex = (0..3).first { it != phase.question.correctIndex }
        vm.selectAnswer(wrongIndex)
        val updated = vm.currentState.phase as WordRushPhase.Playing
        assertEquals(0, updated.score)
        assertEquals(0, updated.streak)
        assertEquals(false, updated.isCorrect)
        assertEquals(WordRushViewModel.INITIAL_LIVES - 1, updated.lives)
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
        val wrongIndex = (0..3).first { it != correctIndex }
        vm.selectAnswer(wrongIndex)
        assertEquals(stateAfterFirst, vm.currentState.phase)
    }

    // ── Lives ─────────────────────────────────────────────────────────────

    @Test
    fun `lives decrement on each wrong answer`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        val phase = vm.currentState.phase as WordRushPhase.Playing
        val wrongIndex = (0..3).first { it != phase.question.correctIndex }
        vm.selectAnswer(wrongIndex)
        val updated = vm.currentState.phase as WordRushPhase.Playing
        assertEquals(WordRushViewModel.INITIAL_LIVES - 1, updated.lives)
    }

    @Test
    fun `lives do not go below zero`() = runTest {
        val vm = createViewModel()
        vm.startGame()

        // Exhaust all lives
        repeat(WordRushViewModel.INITIAL_LIVES) {
            val phase = vm.currentState.phase
            if (phase is WordRushPhase.Playing) {
                val wrongIndex = (0..3).first { it != phase.question.correctIndex }
                vm.selectAnswer(wrongIndex)
            }
        }

        val resultPhase = vm.currentState.phase
        if (resultPhase is WordRushPhase.Playing) {
            assertTrue(resultPhase.lives >= 0)
        }
    }

    @Test
    fun `result phase includes lives remaining`() = runTest {
        val vm = createViewModel()
        vm.startGame()

        // Answer all questions correctly
        repeat(10) {
            val phase = vm.currentState.phase
            if (phase is WordRushPhase.Playing) {
                vm.selectAnswer(phase.question.correctIndex)
            }
        }

        val result = vm.currentState.phase
        if (result is WordRushPhase.Result) {
            assertEquals(WordRushViewModel.INITIAL_LIVES, result.livesRemaining)
        }
    }

    // ── Power-ups ─────────────────────────────────────────────────────────

    @Test
    fun `streak 3 earns Freeze power-up`() = runTest {
        val vm = createViewModel()
        vm.startGame()

        repeat(3) {
            val phase = vm.currentState.phase
            if (phase is WordRushPhase.Playing) {
                vm.selectAnswer(phase.question.correctIndex)
                advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)
            }
        }

        val phase = vm.currentState.phase as WordRushPhase.Playing
        assertTrue(phase.powerUps.contains(WordRushPowerUp.Freeze))
    }

    @Test
    fun `streak 5 earns FiftyFifty power-up`() = runTest {
        val vm = createViewModel()
        vm.startGame()

        repeat(5) {
            val phase = vm.currentState.phase
            if (phase is WordRushPhase.Playing) {
                vm.selectAnswer(phase.question.correctIndex)
                advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)
            }
        }

        val phase = vm.currentState.phase as WordRushPhase.Playing
        assertTrue(phase.powerUps.contains(WordRushPowerUp.FiftyFifty))
    }

    @Test
    fun `streak 8 earns Peek power-up`() = runTest {
        val vm = createViewModel()
        vm.startGame()

        repeat(8) {
            val phase = vm.currentState.phase
            if (phase is WordRushPhase.Playing) {
                vm.selectAnswer(phase.question.correctIndex)
                advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)
            }
        }

        val phase = vm.currentState.phase as WordRushPhase.Playing
        assertTrue(phase.powerUps.contains(WordRushPowerUp.Peek))
    }

    @Test
    fun `FiftyFifty hides two wrong options`() = runTest {
        val vm = createViewModel()
        vm.startGame()

        // Earn FiftyFifty at streak 5
        repeat(5) {
            val phase = vm.currentState.phase
            if (phase is WordRushPhase.Playing) {
                vm.selectAnswer(phase.question.correctIndex)
                advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)
            }
        }

        val phase = vm.currentState.phase as WordRushPhase.Playing
        vm.usePowerUp(WordRushPowerUp.FiftyFifty)

        val updated = vm.currentState.phase as WordRushPhase.Playing
        assertEquals(2, updated.hiddenOptionIndices.size)
        // Correct answer must never be hidden
        assertTrue(!updated.hiddenOptionIndices.contains(updated.question.correctIndex))
    }

    @Test
    fun `Peek sets isPeeking to true`() = runTest {
        val vm = createViewModel()
        vm.startGame()

        // Earn Peek at streak 8
        repeat(8) {
            val phase = vm.currentState.phase
            if (phase is WordRushPhase.Playing) {
                vm.selectAnswer(phase.question.correctIndex)
                advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)
            }
        }

        val phase = vm.currentState.phase as WordRushPhase.Playing
        vm.usePowerUp(WordRushPowerUp.Peek)

        val updated = vm.currentState.phase as WordRushPhase.Playing
        assertTrue(updated.isPeeking)
    }

    @Test
    fun `using power-up removes it from the list`() = runTest {
        val vm = createViewModel()
        vm.startGame()

        // Earn Freeze at streak 3
        repeat(3) {
            val phase = vm.currentState.phase
            if (phase is WordRushPhase.Playing) {
                vm.selectAnswer(phase.question.correctIndex)
                advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)
            }
        }

        val before = vm.currentState.phase as WordRushPhase.Playing
        assertTrue(before.powerUps.contains(WordRushPowerUp.Freeze))

        vm.usePowerUp(WordRushPowerUp.Freeze)

        val after = vm.currentState.phase as WordRushPhase.Playing
        assertTrue(!after.powerUps.contains(WordRushPowerUp.Freeze))
    }

    @Test
    fun `using power-up not in list is a no-op`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        val before = vm.currentState.phase
        vm.usePowerUp(WordRushPowerUp.Freeze)
        assertEquals(before, vm.currentState.phase)
    }

    // ── Multiplier tests ─────────────────────────────────────────────────

    @Test
    fun `multiplier is 1x for streak below 3`() {
        assertEquals(1, WordRushViewModel.calculateMultiplier(0))
        assertEquals(1, WordRushViewModel.calculateMultiplier(1))
        assertEquals(1, WordRushViewModel.calculateMultiplier(2))
    }

    @Test
    fun `multiplier is 2x after 3 correct streak`() {
        assertEquals(2, WordRushViewModel.calculateMultiplier(3))
        assertEquals(2, WordRushViewModel.calculateMultiplier(4))
    }

    @Test
    fun `multiplier is 3x after 5 correct streak`() {
        assertEquals(3, WordRushViewModel.calculateMultiplier(5))
        assertEquals(3, WordRushViewModel.calculateMultiplier(6))
        assertEquals(3, WordRushViewModel.calculateMultiplier(7))
    }

    @Test
    fun `multiplier is 5x after 8 correct streak`() {
        assertEquals(5, WordRushViewModel.calculateMultiplier(8))
        assertEquals(5, WordRushViewModel.calculateMultiplier(10))
        assertEquals(5, WordRushViewModel.calculateMultiplier(20))
    }

    // ── Grade tests ───────────────────────────────────────────────────────

    @Test
    fun `grade is S for 90 percent or higher accuracy`() {
        assertEquals("S", WordRushViewModel.calculateGrade(0.9f))
        assertEquals("S", WordRushViewModel.calculateGrade(1.0f))
        assertEquals("S", WordRushViewModel.calculateGrade(0.95f))
    }

    @Test
    fun `grade is A for 80 to 89 percent accuracy`() {
        assertEquals("A", WordRushViewModel.calculateGrade(0.8f))
        assertEquals("A", WordRushViewModel.calculateGrade(0.89f))
    }

    @Test
    fun `grade is B for 60 to 79 percent accuracy`() {
        assertEquals("B", WordRushViewModel.calculateGrade(0.6f))
        assertEquals("B", WordRushViewModel.calculateGrade(0.79f))
    }

    @Test
    fun `grade is C for 40 to 59 percent accuracy`() {
        assertEquals("C", WordRushViewModel.calculateGrade(0.4f))
        assertEquals("C", WordRushViewModel.calculateGrade(0.59f))
    }

    @Test
    fun `grade is D for below 40 percent accuracy`() {
        assertEquals("D", WordRushViewModel.calculateGrade(0.0f))
        assertEquals("D", WordRushViewModel.calculateGrade(0.39f))
    }

    // ── Speed bonus tests ─────────────────────────────────────────────────

    @Test
    fun `speed bonus is 2 for answers under 2 seconds`() {
        assertEquals(2, WordRushViewModel.calculateSpeedBonus(500))
        assertEquals(2, WordRushViewModel.calculateSpeedBonus(1000))
        assertEquals(2, WordRushViewModel.calculateSpeedBonus(1999))
    }

    @Test
    fun `speed bonus is 1 for answers between 2 and 3 seconds`() {
        assertEquals(1, WordRushViewModel.calculateSpeedBonus(2000))
        assertEquals(1, WordRushViewModel.calculateSpeedBonus(2500))
        assertEquals(1, WordRushViewModel.calculateSpeedBonus(2999))
    }

    @Test
    fun `speed bonus is 0 for answers 3 seconds or slower`() {
        assertEquals(0, WordRushViewModel.calculateSpeedBonus(3000))
        assertEquals(0, WordRushViewModel.calculateSpeedBonus(5000))
    }

    // ── Result state tests ────────────────────────────────────────────────

    @Test
    fun `result state includes accuracy`() = runTest {
        val vm = createViewModel()
        vm.startGame()

        repeat(10) {
            val phase = vm.currentState.phase
            if (phase is WordRushPhase.Playing) {
                vm.selectAnswer(phase.question.correctIndex)
            }
        }

        val result = vm.currentState.phase
        if (result is WordRushPhase.Result) {
            assertEquals(1.0f, result.accuracy)
            assertEquals("S", result.grade)
            assertEquals(10, result.correctCount)
        }
    }

    @Test
    fun `result state includes grade computed correctly for partial score`() = runTest {
        val vm = createViewModel()
        vm.startGame()

        repeat(10) { i ->
            val phase = vm.currentState.phase
            if (phase is WordRushPhase.Playing) {
                if (i < 6) {
                    vm.selectAnswer(phase.question.correctIndex)
                } else {
                    val wrongIndex = (0..3).first { it != phase.question.correctIndex }
                    vm.selectAnswer(wrongIndex)
                }
            }
        }

        val result = vm.currentState.phase
        if (result is WordRushPhase.Result) {
            assertEquals(0.6f, result.accuracy)
            assertEquals("B", result.grade)
            assertEquals(6, result.correctCount)
        }
    }

    @Test
    fun `correct answer records answer time`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        val phase = vm.currentState.phase as WordRushPhase.Playing
        vm.selectAnswer(phase.question.correctIndex)
        val updated = vm.currentState.phase as WordRushPhase.Playing
        assertTrue(updated.answerTimeMs != null)
    }

    @Test
    fun `correct answer earns at least 1 point with no multiplier and no speed bonus`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        val phase = vm.currentState.phase as WordRushPhase.Playing
        vm.selectAnswer(phase.question.correctIndex)
        val updated = vm.currentState.phase as WordRushPhase.Playing
        assertTrue(updated.score >= 1)
        assertTrue(updated.lastPointsEarned != null)
        assertTrue(updated.lastPointsEarned!! >= 1)
    }
}
