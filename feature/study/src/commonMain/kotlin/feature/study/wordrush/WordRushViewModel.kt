package feature.study.wordrush

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.fold
import domain.focus.usecase.ObserveLearningFocusUseCase
import domain.word.usecase.GetWordRushWordsUseCase
import domain.wordrush.model.WordRushDirection
import domain.wordrush.model.WordRushGameRecord
import domain.wordrush.model.WordRushGrade
import domain.wordrush.model.WordRushMiss
import domain.wordrush.model.WordRushQuestion
import domain.wordrush.usecase.GetWordRushInsightsUseCase
import domain.wordrush.usecase.GetWordRushRoundUseCase
import domain.wordrush.usecase.RecordWordRushGameUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Clock

sealed interface WordRushPowerUp {
    data object Freeze : WordRushPowerUp      // Pause timer for 3 s
    data object FiftyFifty : WordRushPowerUp  // Remove 2 wrong options
    data object Peek : WordRushPowerUp        // Flash correct answer for 600 ms
}

sealed interface WordRushPhase {
    data object Idle : WordRushPhase
    data object Loading : WordRushPhase
    data class Playing(
        val question: WordRushQuestion,
        val questionIndex: Int,
        val totalQuestions: Int,
        val streak: Int,
        val score: Int,
        val timeRemainingMs: Long,
        val lives: Int,
        val powerUps: List<WordRushPowerUp> = emptyList(),
        val hiddenOptionIndices: Set<Int> = emptySet(),
        val isPeeking: Boolean = false,
        val isTimerFrozen: Boolean = false,
        val isPaused: Boolean = false,
        val selectedIndex: Int? = null,
        val isCorrect: Boolean? = null,
        val multiplier: Int = 1,
        val lastPointsEarned: Int? = null,
        val answerTimeMs: Long? = null,
        /** Power-up unlocked by the answer just given; shown once during the reveal. */
        val earnedPowerUp: WordRushPowerUp? = null,
    ) : WordRushPhase

    data class Result(
        val score: Int,
        val totalQuestions: Int,
        val answeredCount: Int,
        val correctCount: Int,
        val bestStreak: Int,
        val isNewBest: Boolean,
        val accuracy: Float,
        val avgResponseTimeMs: Long,
        val grade: WordRushGrade,
        val livesRemaining: Int,
        val missedWords: List<WordRushMiss> = emptyList(),
    ) : WordRushPhase {
        val endedByLives: Boolean get() = livesRemaining == 0
    }

    data object Error : WordRushPhase
}

data class WordRushState(
    val phase: WordRushPhase = WordRushPhase.Idle,
    val bestStreak: Int = 0,
    val hasEnoughWords: Boolean = false,
)

sealed interface WordRushEffect {
    data object GameComplete : WordRushEffect
}

@Suppress("TooManyFunctions")
class WordRushViewModel(
    private val getWordRushWordsUseCase: GetWordRushWordsUseCase,
    private val getWordRushRoundUseCase: GetWordRushRoundUseCase,
    private val recordWordRushGameUseCase: RecordWordRushGameUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val getWordRushInsightsUseCase: GetWordRushInsightsUseCase,
    private val observeLearningFocus: ObserveLearningFocusUseCase,
) : BaseViewModel<WordRushState, WordRushEffect>() {

    override fun initialState() = WordRushState()

    private var questions: List<WordRushQuestion> = emptyList()
    private var currentIndex = 0
    private var currentStreak = 0
    private var bestSessionStreak = 0
    private var score = 0
    private var correctCount = 0
    private var answeredCount = 0
    private var gameStartedAt: Long = 0L
    private var responseTimes: MutableList<Long> = mutableListOf()
    private var misses: MutableList<WordRushMiss> = mutableListOf()

    private var timerJob: Job? = null
    private var freezeJob: Job? = null
    private var peekJob: Job? = null
    private var advanceJob: Job? = null

    // Answer time excludes spans where the clock was stopped (Freeze, app in background).
    private var questionStartTimeMs: Long = 0L
    private var clockStoppedAtMs: Long? = null
    private var stoppedDurationMs: Long = 0L

    init {
        analyticsTracker.logScreenView("WordRush")
        // Word availability depends on the learning focus: re-check whenever it changes.
        viewModelScope.launch {
            observeLearningFocus().collectLatest {
                getWordRushWordsUseCase(GetWordRushWordsUseCase.MINIMUM_WORDS).fold(
                    onSuccess = { updateState { copy(hasEnoughWords = true) } },
                    onFailure = { updateState { copy(hasEnoughWords = false) } },
                )
            }
        }
        viewModelScope.launch {
            getWordRushInsightsUseCase(Unit).fold(
                onSuccess = { insights -> updateState { copy(bestStreak = insights.bestStreakEver) } },
                onFailure = { /* keep default 0 — isNewBest comparison degrades gracefully */ },
            )
        }
    }

    fun startGame() {
        if (currentState.phase is WordRushPhase.Loading) return
        cancelRoundJobs()
        updateState { copy(phase = WordRushPhase.Loading) }
        viewModelScope.launch {
            getWordRushRoundUseCase(ROUND_COUNT).fold(
                onSuccess = { round ->
                    // The player closed the game while the round was loading.
                    if (currentState.phase !is WordRushPhase.Loading) return@fold
                    questions = round
                    currentIndex = 0
                    currentStreak = 0
                    bestSessionStreak = 0
                    score = 0
                    correctCount = 0
                    answeredCount = 0
                    responseTimes = mutableListOf()
                    misses = mutableListOf()
                    gameStartedAt = now()
                    analyticsTracker.logEvent(
                        eventName = "word_rush_game_start",
                        parameters = mapOf(
                            "question_count" to round.size,
                            "recall_count" to round.count { it.direction == WordRushDirection.Recall },
                        ),
                    )
                    showQuestion(lives = INITIAL_LIVES, powerUps = emptyList())
                },
                onFailure = {
                    if (currentState.phase is WordRushPhase.Loading) {
                        updateState { copy(phase = WordRushPhase.Error) }
                    }
                },
            )
        }
    }

    fun selectAnswer(index: Int) {
        val phase = currentState.phase
        if (phase !is WordRushPhase.Playing || phase.selectedIndex != null || phase.isPaused) return
        if (index !in phase.question.options.indices || index in phase.hiddenOptionIndices) return

        timerJob?.cancel()
        freezeJob?.cancel()
        peekJob?.cancel()
        val answerTimeMs = elapsedOnQuestionMs()
        responseTimes.add(answerTimeMs)
        answeredCount++

        if (index == phase.question.correctIndex) {
            processCorrectAnswer(phase, index, answerTimeMs)
        } else {
            currentStreak = 0
            misses.add(phase.question.toMiss(chosenAnswer = phase.question.options[index]))
            updateState {
                copy(
                    phase = phase.copy(
                        selectedIndex = index,
                        isCorrect = false,
                        streak = 0,
                        multiplier = 1,
                        lastPointsEarned = null,
                        answerTimeMs = answerTimeMs,
                        lives = (phase.lives - 1).coerceAtLeast(0),
                        isPeeking = false,
                        isTimerFrozen = false,
                    ),
                )
            }
        }
        scheduleAdvance()
    }

    private fun processCorrectAnswer(phase: WordRushPhase.Playing, index: Int, answerTimeMs: Long) {
        currentStreak++
        correctCount++
        if (currentStreak > bestSessionStreak) bestSessionStreak = currentStreak

        val multiplier = calculateMultiplier(currentStreak)
        val pointsEarned = multiplier + calculateSpeedBonus(answerTimeMs)
        score += pointsEarned

        val earnedPowerUp = powerUpForStreak(currentStreak)?.takeIf { it !in phase.powerUps }

        updateState {
            copy(
                phase = phase.copy(
                    selectedIndex = index,
                    isCorrect = true,
                    streak = currentStreak,
                    score = score,
                    multiplier = multiplier,
                    lastPointsEarned = pointsEarned,
                    answerTimeMs = answerTimeMs,
                    powerUps = if (earnedPowerUp != null) phase.powerUps + earnedPowerUp else phase.powerUps,
                    earnedPowerUp = earnedPowerUp,
                    isPeeking = false,
                    isTimerFrozen = false,
                ),
            )
        }
    }

    fun usePowerUp(powerUp: WordRushPowerUp) {
        val phase = currentState.phase
        if (phase !is WordRushPhase.Playing || phase.selectedIndex != null || phase.isPaused) return
        if (!phase.powerUps.contains(powerUp)) return

        val updatedPowerUps = phase.powerUps - powerUp
        when (powerUp) {
            WordRushPowerUp.Freeze -> applyFreeze(phase, updatedPowerUps)
            WordRushPowerUp.FiftyFifty -> applyFiftyFifty(phase, updatedPowerUps)
            WordRushPowerUp.Peek -> applyPeek(phase, updatedPowerUps)
        }
    }

    /** Stops the clock while the app is in the background, so the player doesn't lose lives. */
    fun pause() {
        val phase = currentState.phase
        if (phase !is WordRushPhase.Playing || phase.isPaused) return
        cancelRoundJobs()
        if (phase.selectedIndex == null) stopClock()
        updateState { copy(phase = phase.copy(isPaused = true, isTimerFrozen = false, isPeeking = false)) }
    }

    fun resume() {
        val phase = currentState.phase
        if (phase !is WordRushPhase.Playing || !phase.isPaused) return
        updateState { copy(phase = phase.copy(isPaused = false)) }
        if (phase.selectedIndex != null) {
            scheduleAdvance()
        } else {
            restartClock()
            startTimer(phase.timeRemainingMs)
        }
    }

    fun dismiss() {
        cancelRoundJobs()
        val phase = currentState.phase
        if (phase is WordRushPhase.Playing || phase is WordRushPhase.Loading) {
            analyticsTracker.logEvent(
                eventName = "word_rush_dropped_out",
                parameters = mapOf(
                    "questions_answered" to answeredCount,
                    "score" to score,
                ),
            )
        }
        updateState { copy(phase = WordRushPhase.Idle) }
    }

    private fun applyFreeze(phase: WordRushPhase.Playing, updatedPowerUps: List<WordRushPowerUp>) {
        timerJob?.cancel()
        stopClock()
        updateState { copy(phase = phase.copy(powerUps = updatedPowerUps, isTimerFrozen = true)) }
        freezeJob = viewModelScope.launch {
            delay(FREEZE_DURATION_MS)
            val current = currentState.phase as? WordRushPhase.Playing ?: return@launch
            updateState { copy(phase = current.copy(isTimerFrozen = false)) }
            restartClock()
            startTimer(current.timeRemainingMs)
        }
    }

    private fun applyFiftyFifty(phase: WordRushPhase.Playing, updatedPowerUps: List<WordRushPowerUp>) {
        val visibleWrong = phase.question.options.indices
            .filter { it != phase.question.correctIndex && it !in phase.hiddenOptionIndices }
        // Always leave at least one wrong option, so it stays a choice.
        val toHide = visibleWrong.shuffled().take((visibleWrong.size - 1).coerceAtMost(2))
        if (toHide.isEmpty()) return
        updateState {
            copy(
                phase = phase.copy(
                    powerUps = updatedPowerUps,
                    hiddenOptionIndices = phase.hiddenOptionIndices + toHide,
                ),
            )
        }
    }

    private fun applyPeek(phase: WordRushPhase.Playing, updatedPowerUps: List<WordRushPowerUp>) {
        updateState { copy(phase = phase.copy(powerUps = updatedPowerUps, isPeeking = true)) }
        peekJob = viewModelScope.launch {
            delay(PEEK_DURATION_MS)
            val current = currentState.phase as? WordRushPhase.Playing ?: return@launch
            updateState { copy(phase = current.copy(isPeeking = false)) }
        }
    }

    private fun onTimeUp() {
        val phase = currentState.phase
        if (phase !is WordRushPhase.Playing || phase.selectedIndex != null) return

        currentStreak = 0
        answeredCount++
        misses.add(phase.question.toMiss(chosenAnswer = null))
        updateState {
            copy(
                phase = phase.copy(
                    selectedIndex = TIMED_OUT_INDEX,
                    isCorrect = false,
                    streak = 0,
                    timeRemainingMs = 0,
                    multiplier = 1,
                    lastPointsEarned = null,
                    answerTimeMs = null,
                    lives = (phase.lives - 1).coerceAtLeast(0),
                    isPeeking = false,
                ),
            )
        }
        scheduleAdvance()
    }

    private fun scheduleAdvance() {
        advanceJob?.cancel()
        advanceJob = viewModelScope.launch {
            delay(ANSWER_REVEAL_MS)
            val phase = currentState.phase as? WordRushPhase.Playing ?: return@launch
            if (phase.lives == 0 || currentIndex + 1 >= questions.size) {
                finishGame(phase)
            } else {
                currentIndex++
                showQuestion(lives = phase.lives, powerUps = phase.powerUps)
            }
        }
    }

    private fun showQuestion(lives: Int, powerUps: List<WordRushPowerUp>) {
        questionStartTimeMs = now()
        clockStoppedAtMs = null
        stoppedDurationMs = 0L
        updateState {
            copy(
                phase = WordRushPhase.Playing(
                    question = questions[currentIndex],
                    questionIndex = currentIndex,
                    totalQuestions = questions.size,
                    streak = currentStreak,
                    score = score,
                    timeRemainingMs = TIME_PER_QUESTION_MS,
                    multiplier = calculateMultiplier(currentStreak),
                    lives = lives,
                    powerUps = powerUps,
                ),
            )
        }
        startTimer(TIME_PER_QUESTION_MS)
    }

    private fun startTimer(remainingMs: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var elapsed = 0L
            while (elapsed < remainingMs) {
                delay(TIMER_TICK_MS)
                elapsed += TIMER_TICK_MS
                val phase = currentState.phase
                if (phase !is WordRushPhase.Playing || phase.selectedIndex != null) return@launch
                updateState { copy(phase = phase.copy(timeRemainingMs = (remainingMs - elapsed).coerceAtLeast(0))) }
            }
            onTimeUp()
        }
    }

    private fun finishGame(phase: WordRushPhase.Playing) {
        val livesRemaining = phase.lives
        val durationMs = now() - gameStartedAt
        analyticsTracker.logEvent(
            eventName = "word_rush_game_complete",
            parameters = mapOf(
                "score" to score,
                "total_questions" to questions.size,
                "best_streak" to bestSessionStreak,
                "duration_ms" to durationMs,
                "missed_count" to misses.size,
            ),
        )
        val previousBest = currentState.bestStreak
        val isNewBest = bestSessionStreak > previousBest
        val accuracy = if (answeredCount > 0) correctCount.toFloat() / answeredCount else 0f
        val avgResponseTimeMs = if (responseTimes.isNotEmpty()) responseTimes.average().toLong() else 0L
        val grade = WordRushGrade.fromAccuracy(accuracy)

        updateState {
            copy(
                phase = WordRushPhase.Result(
                    score = score,
                    totalQuestions = questions.size,
                    answeredCount = answeredCount,
                    correctCount = correctCount,
                    bestStreak = bestSessionStreak,
                    isNewBest = isNewBest,
                    accuracy = accuracy,
                    avgResponseTimeMs = avgResponseTimeMs,
                    grade = grade,
                    livesRemaining = livesRemaining,
                    missedWords = misses.toList(),
                ),
                bestStreak = maxOf(previousBest, bestSessionStreak),
            )
        }
        emitEffect(WordRushEffect.GameComplete)

        val record = WordRushGameRecord(
            clientGameId = "wr_${now()}_${(0..9999).random()}",
            score = score,
            totalQuestions = questions.size,
            correctCount = correctCount,
            bestStreak = bestSessionStreak,
            durationMs = durationMs,
            avgResponseMs = avgResponseTimeMs,
            grade = grade,
            livesRemaining = livesRemaining,
            completedNormally = true,
            playedAt = now(),
        )
        viewModelScope.launch { recordWordRushGameUseCase(record) }
    }

    private fun cancelRoundJobs() {
        timerJob?.cancel()
        freezeJob?.cancel()
        peekJob?.cancel()
        advanceJob?.cancel()
    }

    private fun stopClock() {
        if (clockStoppedAtMs == null) clockStoppedAtMs = now()
    }

    private fun restartClock() {
        clockStoppedAtMs?.let { stoppedDurationMs += now() - it }
        clockStoppedAtMs = null
    }

    private fun elapsedOnQuestionMs(): Long {
        val now = now()
        val stoppedNow = clockStoppedAtMs?.let { now - it } ?: 0L
        return (now - questionStartTimeMs - stoppedDurationMs - stoppedNow).coerceAtLeast(0L)
    }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    private fun WordRushQuestion.toMiss(chosenAnswer: String?) =
        WordRushMiss(prompt = prompt, correctAnswer = answer, chosenAnswer = chosenAnswer)

    companion object {
        const val ROUND_COUNT = 10
        const val TIME_PER_QUESTION_MS = 5000L
        const val ANSWER_REVEAL_MS = 1200L
        const val TIMER_TICK_MS = 50L
        const val INITIAL_LIVES = 3
        const val FREEZE_DURATION_MS = 3000L
        const val PEEK_DURATION_MS = 600L
        const val TIMED_OUT_INDEX = -1

        fun calculateMultiplier(streak: Int): Int = when {
            streak >= 8 -> 5
            streak >= 5 -> 3
            streak >= 3 -> 2
            else -> 1
        }

        fun calculateSpeedBonus(answerTimeMs: Long): Int = when {
            answerTimeMs < 2000L -> 2
            answerTimeMs < 3000L -> 1
            else -> 0
        }

        fun powerUpForStreak(streak: Int): WordRushPowerUp? = when (streak) {
            3 -> WordRushPowerUp.Freeze
            5 -> WordRushPowerUp.FiftyFifty
            8 -> WordRushPowerUp.Peek
            else -> null
        }
    }
}
