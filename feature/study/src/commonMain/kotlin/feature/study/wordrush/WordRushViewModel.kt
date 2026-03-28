package feature.study.wordrush

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.fold
import domain.word.model.Word
import domain.word.usecase.GetWordRushWordsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

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

        var pointsEarned = 0

        if (isCorrect) {
            currentStreak++
            correctCount++
            if (currentStreak > bestSessionStreak) bestSessionStreak = currentStreak

            val multiplier = calculateMultiplier(currentStreak)
            val basePoints = 1
            val speedBonus = calculateSpeedBonus(answerTimeMs)
            pointsEarned = (basePoints * multiplier) + speedBonus
            score += pointsEarned

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
                    ),
                )
            }
        } else {
            currentStreak = 0
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
                    ),
                )
            }
        }

        viewModelScope.launch {
            delay(ANSWER_REVEAL_MS)
            advanceOrFinish()
        }
    }

    fun dismiss() {
        timerJob?.cancel()
        updateState { copy(phase = WordRushPhase.Idle) }
    }

    private fun onTimeUp() {
        val phase = currentState.phase
        if (phase !is WordRushPhase.Playing || phase.selectedIndex != null) return

        currentStreak = 0
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
                ),
            )
        }

        viewModelScope.launch {
            delay(ANSWER_REVEAL_MS)
            advanceOrFinish()
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
                ),
            )
        }
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var elapsed = 0L
            while (elapsed < TIME_PER_QUESTION_MS) {
                delay(TIMER_TICK_MS)
                elapsed += TIMER_TICK_MS
                val remaining = (TIME_PER_QUESTION_MS - elapsed).coerceAtLeast(0)
                val phase = currentState.phase
                if (phase !is WordRushPhase.Playing || phase.selectedIndex != null) return@launch
                updateState { copy(phase = phase.copy(timeRemainingMs = remaining)) }
            }
            onTimeUp()
        }
    }

    private fun finishGame() {
        val isNewBest = bestSessionStreak > currentState.bestStreak
        val newBest = if (isNewBest) bestSessionStreak else currentState.bestStreak
        val totalQuestions = questions.size
        val accuracy = if (totalQuestions > 0) correctCount.toFloat() / totalQuestions else 0f
        val avgResponseTimeMs = if (responseTimes.isNotEmpty()) {
            responseTimes.average().toLong()
        } else {
            0L
        }
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
                ),
                bestStreak = newBest,
            )
        }
        emitEffect(WordRushEffect.GameComplete)
    }

    private fun buildQuestions(words: List<Word>): List<WordRushQuestion> {
        return words.map { word ->
            val distractors = words
                .filter { it.id != word.id }
                .shuffled()
                .take(OPTIONS_COUNT - 1)
                .map { it.translation }

            val options = (distractors + word.translation).shuffled()
            val correctIndex = options.indexOf(word.translation)

            WordRushQuestion(
                word = word,
                options = options,
                correctIndex = correctIndex,
            )
        }
    }

    companion object {
        const val ROUND_COUNT = 10
        const val OPTIONS_COUNT = 4
        const val TIME_PER_QUESTION_MS = 5000L
        const val ANSWER_REVEAL_MS = 1200L
        const val TIMER_TICK_MS = 50L

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
