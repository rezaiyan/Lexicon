package feature.study.wordrush

import core.common.Try
import domain.focus.usecase.ObserveLearningFocusUseCase
import domain.word.model.Word
import domain.word.usecase.GetWordRushWordsUseCase
import domain.wordrush.model.WordRushGameRecord
import domain.wordrush.model.WordRushGrade
import domain.wordrush.model.WordRushInsights
import domain.wordrush.model.WordRushMiss
import domain.wordrush.repository.IWordRushRecorder
import domain.wordrush.repository.IWordRushStatsRepository
import domain.wordrush.usecase.GetWordRushInsightsUseCase
import domain.wordrush.usecase.GetWordRushRoundUseCase
import domain.wordrush.usecase.RecordWordRushGameUseCase
import fakes.FakeAnalyticsTracker
import fakes.FakeLearningFocusRepository
import fakes.FakeWordRepository
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Timer, pause and round-end behaviour of [WordRushViewModel]. */
class WordRushRoundLifecycleTest : ViewModelTestBase() {

    private object NoOpRecorder : IWordRushRecorder {
        override suspend fun recordGame(game: WordRushGameRecord): Try<Unit> = Try.success(Unit)
        override suspend fun retryPendingSync(): Try<Unit> = Try.success(Unit)
    }

    private object NoStatsRepository : IWordRushStatsRepository {
        override suspend fun getInsights(): Try<WordRushInsights> =
            Try.failure(IllegalStateException("offline"))
    }

    private fun createViewModel(): WordRushViewModel {
        val words = (1..10).map { i ->
            Word(
                id = i,
                originalWord = "word_$i",
                translation = "translation_$i",
                description = "",
                sourceLanguage = Language.ENGLISH,
                targetLanguage = Language.GERMAN,
                nextReviewDate = 0L,
            )
        }
        val repo = FakeWordRepository().apply { storedWords = words.toMutableList() }
        val observeLearningFocus = ObserveLearningFocusUseCase(repo, FakeLearningFocusRepository())
        return WordRushViewModel(
            getWordRushWordsUseCase = GetWordRushWordsUseCase(repo, observeLearningFocus),
            getWordRushRoundUseCase = GetWordRushRoundUseCase(repo, observeLearningFocus),
            recordWordRushGameUseCase = RecordWordRushGameUseCase(NoOpRecorder),
            analyticsTracker = FakeAnalyticsTracker(),
            getWordRushInsightsUseCase = GetWordRushInsightsUseCase(NoStatsRepository),
            observeLearningFocus = observeLearningFocus,
        )
    }

    private fun WordRushViewModel.playing() = currentState.phase as WordRushPhase.Playing

    private fun WordRushViewModel.answerCorrectly() = selectAnswer(playing().question.correctIndex)

    @Test
    fun `dismiss during answer reveal does not bring the game back`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        vm.answerCorrectly()

        vm.dismiss()
        advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)

        assertIs<WordRushPhase.Idle>(vm.currentState.phase)
    }

    @Test
    fun `freeze used before answering does not restart the next question timer`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        repeat(3) {
            vm.answerCorrectly()
            advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)
        }
        assertTrue(WordRushPowerUp.Freeze in vm.playing().powerUps)

        vm.usePowerUp(WordRushPowerUp.Freeze)
        vm.answerCorrectly()
        advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)
        val nextIndex = vm.playing().questionIndex
        // Past the moment the stale freeze would have fired.
        advanceTimeBy(WordRushViewModel.FREEZE_DURATION_MS)

        val phase = vm.playing()
        assertEquals(nextIndex, phase.questionIndex)
        assertTrue(
            phase.timeRemainingMs < WordRushViewModel.TIME_PER_QUESTION_MS - WordRushViewModel.FREEZE_DURATION_MS / 2,
            "Timer was reset by a stale freeze: ${phase.timeRemainingMs}ms left",
        )
    }

    @Test
    fun `pause stops the timer so no life is lost`() = runTest {
        val vm = createViewModel()
        vm.startGame()

        vm.pause()
        advanceTimeBy(WordRushViewModel.TIME_PER_QUESTION_MS * 3)

        val phase = vm.playing()
        assertTrue(phase.isPaused)
        assertEquals(0, phase.questionIndex)
        assertEquals(WordRushViewModel.INITIAL_LIVES, phase.lives)
        assertNull(phase.selectedIndex)
    }

    @Test
    fun `answers are ignored while paused`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        vm.pause()

        vm.answerCorrectly()

        assertNull(vm.playing().selectedIndex)
    }

    @Test
    fun `resume restarts the timer from where it stopped`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        advanceTimeBy(2000)
        vm.pause()
        val remainingAtPause = vm.playing().timeRemainingMs
        advanceTimeBy(10_000)

        vm.resume()
        assertFalse(vm.playing().isPaused)
        assertEquals(remainingAtPause, vm.playing().timeRemainingMs)

        advanceTimeBy(remainingAtPause + 100)
        assertEquals(WordRushViewModel.INITIAL_LIVES - 1, vm.playing().lives)
    }

    @Test
    fun `pause during answer reveal holds the next question until resume`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        vm.answerCorrectly()
        vm.pause()
        advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS * 3)
        assertEquals(0, vm.playing().questionIndex)

        vm.resume()
        advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)

        assertEquals(1, vm.playing().questionIndex)
    }

    @Test
    fun `out of range answer index is ignored`() = runTest {
        val vm = createViewModel()
        vm.startGame()

        vm.selectAnswer(99)

        assertNull(vm.playing().selectedIndex)
        assertEquals(WordRushViewModel.INITIAL_LIVES, vm.playing().lives)
    }

    @Test
    fun `wrong answers are listed as missed words with the chosen option`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        val expected = List(WordRushViewModel.INITIAL_LIVES) {
            val question = vm.playing().question
            val wrongIndex = question.options.indices.first { it != question.correctIndex }
            vm.selectAnswer(wrongIndex)
            advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)
            WordRushMiss(question.prompt, question.answer, question.options[wrongIndex])
        }

        val result = assertIs<WordRushPhase.Result>(vm.currentState.phase)
        assertEquals(expected, result.missedWords)
        assertTrue(result.endedByLives)
    }

    @Test
    fun `timed out questions count against accuracy and are listed as missed`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        vm.answerCorrectly()
        advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)

        repeat(WordRushViewModel.INITIAL_LIVES) {
            advanceTimeBy(WordRushViewModel.TIME_PER_QUESTION_MS + WordRushViewModel.ANSWER_REVEAL_MS + 200)
        }

        val result = assertIs<WordRushPhase.Result>(vm.currentState.phase)
        assertEquals(4, result.answeredCount)
        assertEquals(0.25f, result.accuracy)
        assertEquals(WordRushGrade.D, result.grade)
        assertTrue(result.missedWords.all { it.chosenAnswer == null })
    }

    @Test
    fun `result reports points score separately from correct count`() = runTest {
        val vm = createViewModel()
        vm.startGame()
        repeat(10) {
            vm.answerCorrectly()
            advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)
        }

        val result = assertIs<WordRushPhase.Result>(vm.currentState.phase)
        assertEquals(10, result.correctCount)
        assertTrue(result.score > result.correctCount, "Multipliers and speed bonus must count")
        assertTrue(result.missedWords.isEmpty())
        assertFalse(result.endedByLives)
    }
}
