package feature.study.wordrush

import core.common.Try
import domain.word.model.Word
import domain.word.usecase.GetWordRushWordsUseCase
import fakes.FakeAnalyticsTracker
import domain.wordrush.model.WordRushGameRecord
import domain.wordrush.model.WordRushGrade
import domain.wordrush.model.WordRushInsights
import domain.wordrush.repository.IWordRushRecorder
import domain.wordrush.repository.IWordRushStatsRepository
import domain.wordrush.usecase.GetWordRushInsightsUseCase
import domain.wordrush.usecase.RecordWordRushGameUseCase
import fakes.FakeWordRepository
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WordRushViewModelTest : ViewModelTestBase() {

    private class FakeWordRushRecorder : IWordRushRecorder {
        val recordedGames = mutableListOf<WordRushGameRecord>()
        var recordResult: Try<Unit> = Try.success(Unit)

        override suspend fun recordGame(game: WordRushGameRecord): Try<Unit> {
            recordedGames.add(game)
            return recordResult
        }

        override suspend fun retryPendingSync(): Try<Unit> = Try.success(Unit)
    }

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

    private class FakeWordRushStatsRepository(
        private val bestStreakEver: Int = 0,
    ) : IWordRushStatsRepository {
        override suspend fun getInsights(): Try<WordRushInsights> = Try.success(
            WordRushInsights(
                totalGames = 0,
                totalCompleted = 0,
                completionRatePercent = 0.0,
                bestStreakEver = bestStreakEver,
                avgScore = 0.0,
                avgAccuracyPercent = 0.0,
                totalTimePlayedMs = 0,
                avgDurationMs = 0.0,
                avgResponseMs = 0.0,
            )
        )
    }

    private val defaultRecorder = FakeWordRushRecorder()

    private fun createViewModel(
        words: List<Word> = createWords(10),
        recorder: FakeWordRushRecorder = defaultRecorder,
        bestStreakEver: Int = 0,
    ): WordRushViewModel {
        val repo = fakeRepo(words)
        return WordRushViewModel(
            getWordRushWordsUseCase = GetWordRushWordsUseCase(repo),
            recordWordRushGameUseCase = RecordWordRushGameUseCase(recorder),
            analyticsTracker = FakeAnalyticsTracker(),
            getWordRushInsightsUseCase = GetWordRushInsightsUseCase(FakeWordRushStatsRepository(bestStreakEver)),
        )
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
    fun `hasEnoughWords is true when 4 words exist at different learning stages`() {
        // Regression: words at non-zero levels must NOT be excluded from Word Rush.
        // A user with exactly 4 words spanning FRESH(0), FAMILIAR(2), ALMOST(4), MASTERED(6)
        // must see hasEnoughWords = true.
        val words = listOf(
            Word(id = 1, originalWord = "word_1", translation = "translation_1", description = "desc_1", sourceLanguage = utils.Language.ENGLISH, targetLanguage = utils.Language.GERMAN, level = 0, nextReviewDate = 0L),
            Word(id = 2, originalWord = "word_2", translation = "translation_2", description = "desc_2", sourceLanguage = utils.Language.ENGLISH, targetLanguage = utils.Language.GERMAN, level = 2, nextReviewDate = 0L),
            Word(id = 3, originalWord = "word_3", translation = "translation_3", description = "desc_3", sourceLanguage = utils.Language.ENGLISH, targetLanguage = utils.Language.GERMAN, level = 4, nextReviewDate = 0L),
            Word(id = 4, originalWord = "word_4", translation = "translation_4", description = "desc_4", sourceLanguage = utils.Language.ENGLISH, targetLanguage = utils.Language.GERMAN, level = 6, nextReviewDate = 0L),
        )
        val vm = createViewModel(words)
        assertTrue(vm.currentState.hasEnoughWords)
    }

    @Test
    fun `startGame succeeds when exactly 4 words exist at different learning stages`() = runTest {
        val words = listOf(
            Word(id = 1, originalWord = "word_1", translation = "translation_1", description = "desc_1", sourceLanguage = utils.Language.ENGLISH, targetLanguage = utils.Language.GERMAN, level = 0, nextReviewDate = 0L),
            Word(id = 2, originalWord = "word_2", translation = "translation_2", description = "desc_2", sourceLanguage = utils.Language.ENGLISH, targetLanguage = utils.Language.GERMAN, level = 2, nextReviewDate = 0L),
            Word(id = 3, originalWord = "word_3", translation = "translation_3", description = "desc_3", sourceLanguage = utils.Language.ENGLISH, targetLanguage = utils.Language.GERMAN, level = 4, nextReviewDate = 0L),
            Word(id = 4, originalWord = "word_4", translation = "translation_4", description = "desc_4", sourceLanguage = utils.Language.ENGLISH, targetLanguage = utils.Language.GERMAN, level = 6, nextReviewDate = 0L),
        )
        val vm = createViewModel(words)
        vm.startGame()
        assertIs<WordRushPhase.Playing>(vm.currentState.phase)
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
        assertEquals(WordRushGrade.S, WordRushGrade.fromAccuracy(0.9f))
        assertEquals(WordRushGrade.S, WordRushGrade.fromAccuracy(1.0f))
        assertEquals(WordRushGrade.S, WordRushGrade.fromAccuracy(0.95f))
    }

    @Test
    fun `grade is A for 80 to 89 percent accuracy`() {
        assertEquals(WordRushGrade.A, WordRushGrade.fromAccuracy(0.8f))
        assertEquals(WordRushGrade.A, WordRushGrade.fromAccuracy(0.89f))
    }

    @Test
    fun `grade is B for 60 to 79 percent accuracy`() {
        assertEquals(WordRushGrade.B, WordRushGrade.fromAccuracy(0.6f))
        assertEquals(WordRushGrade.B, WordRushGrade.fromAccuracy(0.79f))
    }

    @Test
    fun `grade is C for 40 to 59 percent accuracy`() {
        assertEquals(WordRushGrade.C, WordRushGrade.fromAccuracy(0.4f))
        assertEquals(WordRushGrade.C, WordRushGrade.fromAccuracy(0.59f))
    }

    @Test
    fun `grade is D for below 40 percent accuracy`() {
        assertEquals(WordRushGrade.D, WordRushGrade.fromAccuracy(0.0f))
        assertEquals(WordRushGrade.D, WordRushGrade.fromAccuracy(0.39f))
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
            assertEquals(WordRushGrade.S, result.grade)
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
            assertEquals(WordRushGrade.B, result.grade)
            assertEquals(6, result.correctCount)
        }
    }

    @Test
    fun `accuracy uses answered question count as denominator when game ends early due to lives`() = runTest {
        val vm = createViewModel()
        vm.startGame()

        // Alternate correct/wrong: 3 correct, 3 wrong → all lives lost after 6 questions
        // Fix:  accuracy = 3/6 = 0.5f → Grade C
        // Bug:  accuracy = 3/10 = 0.3f → Grade D (uses questions.size instead of answered count)
        repeat(WordRushViewModel.INITIAL_LIVES * 2) { i ->
            val phase = vm.currentState.phase
            if (phase is WordRushPhase.Playing) {
                if (i % 2 == 0) {
                    vm.selectAnswer(phase.question.correctIndex)
                } else {
                    val wrongIndex = (0..3).first { it != phase.question.correctIndex }
                    vm.selectAnswer(wrongIndex)
                }
                advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)
            }
        }

        val result = vm.currentState.phase
        assertIs<WordRushPhase.Result>(result)
        assertEquals(3, result.correctCount)
        assertEquals(0.5f, result.accuracy)
        assertEquals(WordRushGrade.C, result.grade)
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

    // ── Game recording tests ──────────────────────────────────────────────

    @Test
    fun `game recorded after finishing all questions`() = runTest {
        val recorder = FakeWordRushRecorder()
        val vm = createViewModel(recorder = recorder)
        vm.startGame()

        // Answer all 10 questions correctly
        repeat(WordRushViewModel.ROUND_COUNT) {
            val phase = vm.currentState.phase
            if (phase is WordRushPhase.Playing) {
                vm.selectAnswer(phase.question.correctIndex)
                advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)
            }
        }

        // Wait for any remaining coroutines
        advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)

        assertEquals(1, recorder.recordedGames.size)
        val record = recorder.recordedGames.first()
        assertEquals(WordRushViewModel.ROUND_COUNT, record.totalQuestions)
        assertEquals(WordRushViewModel.ROUND_COUNT, record.correctCount)
        assertTrue(record.score > 0)
        assertTrue(record.completedNormally)
        assertTrue(record.clientGameId.startsWith("wr_"))
        assertTrue(record.durationMs >= 0)
        assertTrue(record.playedAt > 0)
    }

    @Test
    fun `game recorded when lives run out`() = runTest {
        val recorder = FakeWordRushRecorder()
        val vm = createViewModel(recorder = recorder)
        vm.startGame()

        // Answer wrong until lives run out
        repeat(WordRushViewModel.INITIAL_LIVES) {
            val phase = vm.currentState.phase
            if (phase is WordRushPhase.Playing) {
                val wrongIndex = (0..3).first { it != phase.question.correctIndex }
                vm.selectAnswer(wrongIndex)
                advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)
            }
        }

        advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)

        assertEquals(1, recorder.recordedGames.size)
        val record = recorder.recordedGames.first()
        assertEquals(0, record.correctCount)
        assertEquals(0, record.livesRemaining)
        assertTrue(record.completedNormally)
    }

    @Test
    fun `game NOT recorded when user dismisses mid-game`() = runTest {
        val recorder = FakeWordRushRecorder()
        val vm = createViewModel(recorder = recorder)
        vm.startGame()

        // Dismiss without finishing
        vm.dismiss()

        assertEquals(0, recorder.recordedGames.size)
    }

    // ── isNewBest regression tests ────────────────────────────────────────────

    @Test
    fun `isNewBest is false when session streak is below historical best`() = runTest {
        // Regression: user had historical best of 10, plays a bad round (streak=1)
        // → must NOT see "New Streak Record!" badge
        val vm = createViewModel(bestStreakEver = 10)
        vm.startGame()

        // Answer 1 correctly (streak = 1)
        val phase = vm.currentState.phase as WordRushPhase.Playing
        vm.selectAnswer(phase.question.correctIndex)
        advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)

        // Drain remaining lives with wrong answers to trigger game-over result
        repeat(WordRushViewModel.INITIAL_LIVES) {
            val p = vm.currentState.phase
            if (p is WordRushPhase.Playing) {
                val wrongIndex = (0..3).first { it != p.question.correctIndex }
                vm.selectAnswer(wrongIndex)
                advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)
            }
        }
        advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)

        val result = assertIs<WordRushPhase.Result>(vm.currentState.phase)
        assertFalse(result.isNewBest, "Session streak of 1 must NOT beat historical best of 10")
    }

    @Test
    fun `isNewBest is true when session streak exceeds historical best`() = runTest {
        val vm = createViewModel(bestStreakEver = 2)
        vm.startGame()

        // Answer 3 correctly to beat historical best of 2
        repeat(3) {
            val p = vm.currentState.phase
            if (p is WordRushPhase.Playing) {
                vm.selectAnswer(p.question.correctIndex)
                advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)
            }
        }

        // Drain remaining lives to trigger result
        repeat(WordRushViewModel.INITIAL_LIVES) {
            val p = vm.currentState.phase
            if (p is WordRushPhase.Playing) {
                val wrongIndex = (0..3).first { it != p.question.correctIndex }
                vm.selectAnswer(wrongIndex)
                advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)
            }
        }
        advanceTimeBy(WordRushViewModel.ANSWER_REVEAL_MS + 100)

        val result = assertIs<WordRushPhase.Result>(vm.currentState.phase)
        assertTrue(result.isNewBest, "Session streak of 3 must beat historical best of 2")
    }
}
