package feature.study.wordrush

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.fold
import domain.word.model.Word
import domain.word.usecase.GetWordRushWordsUseCase
import domain.wordrush.model.WordRushGameRecord
import domain.wordrush.usecase.RecordWordRushGameUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

sealed interface WordRushPowerUp {
    data object Freeze : WordRushPowerUp      // Pause timer for 3 s
    data object FiftyFifty : WordRushPowerUp  // Remove 2 wrong options
    data object Peek : WordRushPowerUp        // Flash correct answer for 600 ms
}

data class WordRushQuestion(
    val word: Word,
    val options: List<String>,
    val correctIndex: Int,
)

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
        val selectedIndex: Int? = null,
        val isCorrect: Boolean? = null,
        val multiplier: Int = 1,
        val lastPointsEarned: Int? = null,
        val answerTimeMs: Long? = null,
    ) : WordRushPhase

    data class Result(
        val score: Int,
        val totalQuestions: Int,
        val correctCount: Int,
        val bestStreak: Int,
        val isNewBest: Boolean,
        val accuracy: Float,
        val avgResponseTimeMs: Long,
        val grade: String,
        val livesRemaining: Int,
    ) : WordRushPhase

    data class Error(val message: String) : WordRushPhase
}

data class WordRushState(
    val phase: WordRushPhase = WordRushPhase.Idle,
    val bestStreak: Int = 0,
    val hasEnoughWords: Boolean = false,
)

sealed interface WordRushEffect {
    data object GameComplete : WordRushEffect
}

class WordRushViewModel(
    private val getWordRushWordsUseCase: GetWordRushWordsUseCase,
    private val recordWordRushGameUseCase: RecordWordRushGameUseCase,
) : BaseViewModel<WordRushState, WordRushEffect>() {

    override fun initialState() = WordRushState()

    private var allWords: List<Word> = emptyList()
    private var questions: List<WordRushQuestion> = emptyList()
    private var currentIndex = 0
    private var currentStreak = 0
    private var bestSessionStreak = 0
    private var score = 0
    private var correctCount = 0
    private var timerJob: Job? = null
    private var questionStartTimeMs: Long = 0L
    private var responseTimes: MutableList<Long> = mutableListOf()
    private var gameStartedAt: Long = 0L

    init {
        checkWordAvailability()
    }

    private fun checkWordAvailability() {
        viewModelScope.launch {
            getWordRushWordsUseCase(GetWordRushWordsUseCase.MINIMUM_WORDS).fold(
                onSuccess = { updateState { copy(hasEnoughWords = true) } },
                onFailure = { updateState { copy(hasEnoughWords = false) } },
            )
        }
    }

    fun startGame() {
        updateState { copy(phase = WordRushPhase.Loading) }
        viewModelScope.launch {
            getWordRushWordsUseCase(ROUND_COUNT).fold(
                onSuccess = { words ->
                    allWords = words
                    questions = buildQuestions(words)
                    currentIndex = 0
                    currentStreak = 0
                    bestSessionStreak = 0
                    score = 0
                    correctCount = 0
                    responseTimes = mutableListOf()
                    gameStartedAt = Clock.System.now().toEpochMilliseconds()
                    showQuestion()
                },
                onFailure = { error ->
                    updateState { copy(phase = WordRushPhase.Error(error.message ?: "Failed to load words")) }
                },
            )
        }
    }

    fun selectAnswer(index: Int) {
        val phase = currentState.phase
        if (phase !is WordRushPhase.Playing || phase.selectedIndex != null) return

        timerJob?.cancel()
        val isCorrect = index == phase.question.correctIndex
        val answerTimeMs = Clock.System.now().toEpochMilliseconds() - questionStartTimeMs
        responseTimes.add(answerTimeMs)

        var isGameOver = false

        if (isCorrect) {
            currentStreak++
            correctCount++
            if (currentStreak > bestSessionStreak) bestSessionStreak = currentStreak

            val multiplier = calculateMultiplier(currentStreak)
            val speedBonus = calculateSpeedBonus(answerTimeMs)
            val pointsEarned = (1 * multiplier) + speedBonus
            score += pointsEarned

            val earnedPowerUp: WordRushPowerUp? = when (currentStreak) {
                3 -> WordRushPowerUp.Freeze
                5 -> WordRushPowerUp.FiftyFifty
                8 -> WordRushPowerUp.Peek
                else -> null
            }
            val updatedPowerUps = if (earnedPowerUp != null && !phase.powerUps.contains(earnedPowerUp)) {
                phase.powerUps + earnedPowerUp
            } else {
                phase.powerUps
            }

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
                        powerUps = updatedPowerUps,
                    ),
                )
            }
        } else {
            currentStreak = 0
            val newLives = (phase.lives - 1).coerceAtLeast(0)
            isGameOver = newLives == 0
            updateState {
                copy(
                    phase = phase.copy(
                        selectedIndex = index,
                        isCorrect = false,
                        streak = 0,
                        score = score,
                        multiplier = 1,
                        lastPointsEarned = null,
                        answerTimeMs = answerTimeMs,
                        lives = newLives,
                    ),
                )
            }
        }

        viewModelScope.launch {
            delay(ANSWER_REVEAL_MS)
            if (isGameOver) finishGame() else advanceOrFinish()
        }
    }

    fun usePowerUp(powerUp: WordRushPowerUp) {
        val phase = currentState.phase
        if (phase !is WordRushPhase.Playing || phase.selectedIndex != null) return
        if (!phase.powerUps.contains(powerUp)) return

        val updatedPowerUps = phase.powerUps - powerUp
        when (powerUp) {
            WordRushPowerUp.Freeze -> applyFreeze(phase, updatedPowerUps)
            WordRushPowerUp.FiftyFifty -> applyFiftyFifty(phase, updatedPowerUps)
            WordRushPowerUp.Peek -> applyPeek(phase, updatedPowerUps)
        }
    }

    fun dismiss() {
        timerJob?.cancel()
        updateState { copy(phase = WordRushPhase.Idle) }
    }

    private fun applyFreeze(phase: WordRushPhase.Playing, updatedPowerUps: List<WordRushPowerUp>) {
        val frozenTime = phase.timeRemainingMs
        timerJob?.cancel()
        updateState { copy(phase = phase.copy(powerUps = updatedPowerUps, isTimerFrozen = true)) }
        viewModelScope.launch {
            delay(FREEZE_DURATION_MS)
            val currentPhase = currentState.phase
            if (currentPhase !is WordRushPhase.Playing || currentPhase.selectedIndex != null) return@launch
            updateState { copy(phase = currentPhase.copy(isTimerFrozen = false)) }
            startTimer(frozenTime)
        }
    }

    private fun applyFiftyFifty(phase: WordRushPhase.Playing, updatedPowerUps: List<WordRushPowerUp>) {
        val wrongIndices = (0 until OPTIONS_COUNT)
            .filter { it != phase.question.correctIndex && !phase.hiddenOptionIndices.contains(it) }
            .shuffled()
            .take(2)
            .toSet()
        updateState { copy(phase = phase.copy(powerUps = updatedPowerUps, hiddenOptionIndices = wrongIndices)) }
    }

    private fun applyPeek(phase: WordRushPhase.Playing, updatedPowerUps: List<WordRushPowerUp>) {
        updateState { copy(phase = phase.copy(powerUps = updatedPowerUps, isPeeking = true)) }
        viewModelScope.launch {
            delay(PEEK_DURATION_MS)
            val currentPhase = currentState.phase
            if (currentPhase is WordRushPhase.Playing) {
                updateState { copy(phase = currentPhase.copy(isPeeking = false)) }
            }
        }
    }

    private fun onTimeUp() {
        val phase = currentState.phase
        if (phase !is WordRushPhase.Playing || phase.selectedIndex != null) return

        currentStreak = 0
        val newLives = (phase.lives - 1).coerceAtLeast(0)
        val isGameOver = newLives == 0
        updateState {
            copy(
                phase = phase.copy(
                    selectedIndex = -1,
                    isCorrect = false,
                    streak = 0,
                    timeRemainingMs = 0,
                    multiplier = 1,
                    lastPointsEarned = null,
                    answerTimeMs = null,
                    lives = newLives,
                ),
            )
        }

        viewModelScope.launch {
            delay(ANSWER_REVEAL_MS)
            if (isGameOver) finishGame() else advanceOrFinish()
        }
    }

    private fun advanceOrFinish() {
        currentIndex++
        if (currentIndex < questions.size) {
            showQuestion()
        } else {
            finishGame()
        }
    }

    private fun showQuestion() {
        val question = questions[currentIndex]
        questionStartTimeMs = Clock.System.now().toEpochMilliseconds()
        val prevPhase = currentState.phase as? WordRushPhase.Playing
        val lives = prevPhase?.lives ?: INITIAL_LIVES
        val powerUps = prevPhase?.powerUps ?: emptyList()
        updateState {
            copy(
                phase = WordRushPhase.Playing(
                    question = question,
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
        startTimer()
    }

    private fun startTimer(remainingMs: Long = TIME_PER_QUESTION_MS) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var elapsed = 0L
            while (elapsed < remainingMs) {
                delay(TIMER_TICK_MS)
                elapsed += TIMER_TICK_MS
                val remaining = (remainingMs - elapsed).coerceAtLeast(0)
                val phase = currentState.phase
                if (phase !is WordRushPhase.Playing || phase.selectedIndex != null) return@launch
                updateState { copy(phase = phase.copy(timeRemainingMs = remaining)) }
            }
            onTimeUp()
        }
    }

    private fun finishGame() {
        val phase = currentState.phase as? WordRushPhase.Playing
        val livesRemaining = phase?.lives ?: 0
        val isNewBest = bestSessionStreak > currentState.bestStreak
        val newBest = if (isNewBest) bestSessionStreak else currentState.bestStreak
        val totalQuestions = questions.size
        val accuracy = if (totalQuestions > 0) correctCount.toFloat() / totalQuestions else 0f
        val avgResponseTimeMs = if (responseTimes.isNotEmpty()) responseTimes.average().toLong() else 0L
        val grade = calculateGrade(accuracy)

        updateState {
            copy(
                phase = WordRushPhase.Result(
                    score = score,
                    totalQuestions = totalQuestions,
                    correctCount = correctCount,
                    bestStreak = bestSessionStreak,
                    isNewBest = isNewBest,
                    accuracy = accuracy,
                    avgResponseTimeMs = avgResponseTimeMs,
                    grade = grade,
                    livesRemaining = livesRemaining,
                ),
                bestStreak = newBest,
            )
        }
        emitEffect(WordRushEffect.GameComplete)

        val record = WordRushGameRecord(
            clientGameId = generateClientGameId(),
            score = score,
            totalQuestions = totalQuestions,
            correctCount = correctCount,
            bestStreak = bestSessionStreak,
            durationMs = Clock.System.now().toEpochMilliseconds() - gameStartedAt,
            avgResponseMs = avgResponseTimeMs,
            grade = grade,
            livesRemaining = livesRemaining,
            completedNormally = true,
            playedAt = Clock.System.now().toEpochMilliseconds(),
        )
        viewModelScope.launch { recordWordRushGameUseCase(record) }
    }

    private fun generateClientGameId(): String =
        "wr_${Clock.System.now().toEpochMilliseconds()}_${(0..9999).random()}"

    private fun buildQuestions(words: List<Word>): List<WordRushQuestion> {
        return words.map { word ->
            val distractors = words
                .filter { it.id != word.id }
                .shuffled()
                .take(OPTIONS_COUNT - 1)
                .map { it.translation }
            val options = (distractors + word.translation).shuffled()
            val correctIndex = options.indexOf(word.translation)
            WordRushQuestion(word = word, options = options, correctIndex = correctIndex)
        }
    }

    companion object {
        const val ROUND_COUNT = 10
        const val OPTIONS_COUNT = 4
        const val TIME_PER_QUESTION_MS = 5000L
        const val ANSWER_REVEAL_MS = 1200L
        const val TIMER_TICK_MS = 50L
        const val INITIAL_LIVES = 3
        const val FREEZE_DURATION_MS = 3000L
        const val PEEK_DURATION_MS = 600L

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

        fun calculateGrade(accuracy: Float): String = when {
            accuracy >= 0.9f -> "S"
            accuracy >= 0.8f -> "A"
            accuracy >= 0.6f -> "B"
            accuracy >= 0.4f -> "C"
            else -> "D"
        }
    }
}
