package feature.study

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.UiState
import core.common.fold
import core.common.onFailure
import core.common.onSuccess
import domain.analytics.usecase.EndStudySessionUseCase
import domain.analytics.usecase.RecordReviewEventUseCase
import domain.analytics.usecase.StartStudySessionUseCase
import domain.settings.usecase.GetReviewSettingsUseCase
import domain.streak.usecase.RecordStreakActivityUseCase
import domain.tts.model.TtsState
import domain.tts.repository.ITtsRepository
import domain.tts.usecase.SpeakWordUseCase
import domain.word.model.LearningStage
import domain.word.model.Word
import domain.word.usecase.DeleteWordUseCase
import domain.word.usecase.GetDueWordsUseCase
import domain.word.usecase.GetWordsByStageUseCase
import domain.word.usecase.ReviewWordUseCase
import domain.word.usecase.UpdateWordUseCase
import expects.logNetwork
import feature.study.model.ReviewScreenState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class ReviewState(
    val review: ReviewScreenState = ReviewScreenState(),
    val ttsState: TtsState = TtsState.Idle,
)

sealed class ReviewEffect {
    data class StartReview(val firstWord: Word) : ReviewEffect()
}

class ReviewViewModel(
    private val getDueWordsUseCase: GetDueWordsUseCase,
    private val getWordsByStageUseCase: GetWordsByStageUseCase,
    private val reviewWordUseCase: ReviewWordUseCase,
    private val updateWordUseCase: UpdateWordUseCase,
    private val deleteWordUseCase: DeleteWordUseCase,
    private val recordStreakActivityUseCase: RecordStreakActivityUseCase,
    private val speakWordUseCase: SpeakWordUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val startStudySessionUseCase: StartStudySessionUseCase,
    private val endStudySessionUseCase: EndStudySessionUseCase,
    private val recordReviewEventUseCase: RecordReviewEventUseCase,
    private val getReviewSettingsUseCase: GetReviewSettingsUseCase,
    ttsRepository: ITtsRepository,
) : BaseViewModel<ReviewState, ReviewEffect>() {

    override fun initialState() = ReviewState()

    private var currentSessionId: String? = null
    private var sessionStartTime: Long = 0L
    private var cardShownTimestamp: Long = 0L
    private var reviewedCardCount: Int = 0
    private var correctCardCount: Int = 0
    private var incorrectCardCount: Int = 0

    init {
        observeTtsState(ttsRepository)
    }

    private fun observeTtsState(ttsRepository: ITtsRepository) {
        viewModelScope.launch {
            ttsRepository.ttsState.collect { state ->
                updateState { copy(ttsState = state) }
            }
        }
    }

    fun startReview() {
        viewModelScope.launch {
            getDueWordsUseCase()
                .catch { /* review unavailable */ }
                .firstOrNull()
                ?.firstOrNull()
                ?.let { emitEffect(ReviewEffect.StartReview(it)) }
        }
    }

    fun startDueReview() {
        viewModelScope.launch {
            beginAnalyticsSession("REVIEW")
            updateState { copy(review = review.copy(wordListState = UiState.Loading)) }
            getDueWordsUseCase()
                .map<List<Word>, UiState<List<Word>>> { UiState.Loaded(it) }
                .catch { e -> emit(UiState.Error(e.message ?: "Failed to load words")) }
                .first()
                .let { state ->
                    updateState { copy(review = review.copy(wordListState = state)) }
                    markCardShown()
                }
        }
    }

    fun loadWordsByStage(stage: LearningStage) {
        viewModelScope.launch {
            updateState { copy(review = review.copy(wordListState = UiState.Loading)) }
            getWordsByStageUseCase(stage)
                .map<List<Word>, UiState<List<Word>>> { UiState.Loaded(it) }
                .catch { e -> emit(UiState.Error(e.message ?: "Failed to load words")) }
                .first()
                .let { state ->
                    updateState { copy(review = review.copy(wordListState = state)) }
                }
        }
    }

    fun startStageReview(stage: LearningStage) {
        loadWordsByStage(stage)
        beginAnalyticsSession("BROWSE")
        markCardShown()
        val wordListState = currentState.review.wordListState
        val cardCount = if (wordListState is UiState.Loaded) wordListState.value.size else 0
        analyticsTracker.logReviewSessionStart(cardCount = cardCount)
    }

    fun reviewWord(word: Word, quality: Int) {
        viewModelScope.launch {
            val previousLevel = word.level
            val responseTime = Clock.System.now().toEpochMilliseconds() - cardShownTimestamp
            reviewWordUseCase(word, quality)

            // Compute new level matching ReviewWordUseCase's SRS logic
            val wasCorrect = quality >= 1
            if (wasCorrect) correctCardCount++ else incorrectCardCount++
            reviewedCardCount++

            val newLevel = computeNewLevel(previousLevel, quality)

            currentSessionId?.let { sid ->
                recordReviewEventUseCase(
                    RecordReviewEventUseCase.Params(
                        sessionId = sid,
                        wordId = word.id,
                        wordText = word.originalWord,
                        wordTranslation = word.translation,
                        sourceLanguage = word.sourceLanguage.code,
                        targetLanguage = word.targetLanguage.code,
                        rating = quality,
                        previousLevel = previousLevel,
                        newLevel = newLevel,
                        responseTimeMs = responseTime,
                        reviewedAt = Clock.System.now().toEpochMilliseconds(),
                    )
                )
            }

            markCardShown()

            analyticsTracker.logWordReviewed(
                rating = quality,
                wordLevel = word.level,
                wasCorrect = wasCorrect
            )
        }
    }

    fun updateWord(word: Word) {
        viewModelScope.launch {
            updateWordUseCase(word).onSuccess { updatedWord ->
                val wordListState = currentState.review.wordListState
                if (wordListState is UiState.Loaded) {
                    val updatedWords = wordListState.value.map {
                        if (it.id == word.id) updatedWord else it
                    }
                    updateState {
                        copy(review = review.copy(wordListState = UiState.Loaded(updatedWords)))
                    }
                }
                analyticsTracker.logEvent("word_updated_in_review")
            }.onFailure { error ->
                analyticsTracker.logNonFatalError(
                    message = "Word update failed in review",
                    additionalInfo = mapOf("error" to (error.message ?: "unknown"))
                )
            }
        }
    }

    fun deleteWord(wordId: Int) {
        viewModelScope.launch {
            deleteWordUseCase(wordId).onSuccess {
                val wordListState = currentState.review.wordListState
                if (wordListState is UiState.Loaded) {
                    val updatedWords = wordListState.value.filterNot { it.id == wordId }
                    updateState {
                        copy(review = review.copy(wordListState = UiState.Loaded(updatedWords)))
                    }
                }
                analyticsTracker.logEvent("word_deleted_in_review")
            }.onFailure { error ->
                analyticsTracker.logNonFatalError(
                    message = "Word deletion failed in review",
                    additionalInfo = mapOf("error" to (error.message ?: "unknown"))
                )
            }
        }
    }

    fun loadWords() {
        startDueReview()
    }

    fun onReviewSessionComplete() {
        val wordListState = currentState.review.wordListState
        val count = if (wordListState is UiState.Loaded) wordListState.value.size else 0
        viewModelScope.launch {
            endAnalyticsSession(completedNormally = true)
            if (count > 0) recordActivity(count)
            updateState { copy(review = ReviewScreenState()) }
        }
    }

    private suspend fun recordActivity(count: Int) {
        recordStreakActivityUseCase(count)
            .onSuccess { logNetwork("RecordActivity", "Success, count=$count") }
            .onFailure { logNetwork("RecordActivity", "Failed, count=$count") }
    }

    fun speakWord(text: String, languageCode: String) {
        viewModelScope.launch {
            val resolvedCode = resolveLanguageCode(text, languageCode)
            logNetwork("TTS", "speakWord: text='$text' wordLang='$languageCode' resolved='$resolvedCode'")
            speakWordUseCase(text, resolvedCode)
        }.invokeOnCompletion { error ->
            if (error != null) {
                logNetwork("TTS", "Error: ${error.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // End session as abandoned if still active when ViewModel is cleared
        if (currentSessionId != null) {
            // Cannot launch coroutine from onCleared since viewModelScope is cancelled.
            // The session will remain unfinished in the DB — the next sync or app
            // launch can detect and close orphaned sessions. This is acceptable for
            // a best-effort analytics system.
            currentSessionId = null
        }
    }

    private suspend fun computeNewLevel(previousLevel: Int, quality: Int): Int {
        val settings = getReviewSettingsUseCase(Unit)
        val forgotPenalty = settings.fold(onSuccess = { it.forgotPenalty }, onFailure = { 2 })
        val successesToAdvance = settings.fold(onSuccess = { it.successesToAdvance }, onFailure = { 1 })
        return when {
            quality == 0 -> maxOf(0, previousLevel - forgotPenalty)
            // With default successesToAdvance=1, always advances on first success
            successesToAdvance <= 1 && previousLevel < 6 -> minOf(6, previousLevel + 1)
            // With higher thresholds, we can't know the repetition count here,
            // so conservatively assume level stays the same
            previousLevel < 6 -> previousLevel
            else -> previousLevel // Already at max
        }
    }

    private fun beginAnalyticsSession(reviewType: String) {
        val sessionId = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString() +
            "-" + (0..999999).random().toString().padStart(6, '0')
        currentSessionId = sessionId
        sessionStartTime = Clock.System.now().toEpochMilliseconds()
        reviewedCardCount = 0
        correctCardCount = 0
        incorrectCardCount = 0
        viewModelScope.launch {
            startStudySessionUseCase(StartStudySessionUseCase.Params(sessionId, reviewType))
        }
    }

    private suspend fun endAnalyticsSession(completedNormally: Boolean) {
        val sid = currentSessionId ?: return
        val now = Clock.System.now().toEpochMilliseconds()
        endStudySessionUseCase(
            EndStudySessionUseCase.Params(
                sessionId = sid,
                endedAt = now,
                durationMs = now - sessionStartTime,
                totalCards = reviewedCardCount,
                correctCount = correctCardCount,
                incorrectCount = incorrectCardCount,
                completedNormally = completedNormally,
            )
        )
        currentSessionId = null
    }

    private fun markCardShown() {
        cardShownTimestamp = Clock.System.now().toEpochMilliseconds()
    }

    private fun resolveLanguageCode(text: String, languageCode: String): String {
        if (languageCode.isNotBlank()) return languageCode

        val words = (currentState.review.wordListState as? UiState.Loaded<List<Word>>)?.value
            ?: return languageCode

        val isTargetSide = words.any { it.originalWord == text }

        val languageCodes = words.map { word ->
            if (isTargetSide) word.targetLanguage.code else word.sourceLanguage.code
        }

        return languageCodes
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?: languageCode
    }
}
