package feature.study.wordrush

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.fold
import domain.word.model.Word
import domain.word.usecase.GetWordRushWordsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    ) : WordRushPhase

    data class Result(
        val score: Int,
        val totalQuestions: Int,
        val bestStreak: Int,
        val isNewBest: Boolean,
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
    private var timerJob: Job? = null

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

        if (isCorrect) {
            currentStreak++
            score++
            if (currentStreak > bestSessionStreak) bestSessionStreak = currentStreak
        } else {
            currentStreak = 0
        }

        updateState {
            copy(
                phase = phase.copy(
                    selectedIndex = index,
                    isCorrect = isCorrect,
                    streak = currentStreak,
                    score = score,
                ),
            )
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
        updateState {
            copy(
                phase = WordRushPhase.Playing(
                    question = question,
                    questionIndex = currentIndex,
                    totalQuestions = questions.size,
                    streak = currentStreak,
                    score = score,
                    timeRemainingMs = TIME_PER_QUESTION_MS,
                ),
            )
        }
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val startTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            while (true) {
                delay(TIMER_TICK_MS)
                val elapsed = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - startTime
                val remaining = (TIME_PER_QUESTION_MS - elapsed).coerceAtLeast(0)
                val phase = currentState.phase
                if (phase is WordRushPhase.Playing && phase.selectedIndex == null) {
                    updateState { copy(phase = phase.copy(timeRemainingMs = remaining)) }
                    if (remaining <= 0) {
                        onTimeUp()
                        break
                    }
                } else {
                    break
                }
            }
        }
    }

    private fun finishGame() {
        val isNewBest = bestSessionStreak > currentState.bestStreak
        val newBest = if (isNewBest) bestSessionStreak else currentState.bestStreak
        updateState {
            copy(
                phase = WordRushPhase.Result(
                    score = score,
                    totalQuestions = questions.size,
                    bestStreak = bestSessionStreak,
                    isNewBest = isNewBest,
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
    }
}
