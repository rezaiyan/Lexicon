package feature.study

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.UiState
import core.common.fold
import core.common.onFailure
import core.common.onSuccess
import domain.analytics.model.ReviewEventParams
import domain.analytics.usecase.EndStudySessionUseCase
import domain.analytics.usecase.RecordReviewEventUseCase
import domain.analytics.usecase.StartStudySessionUseCase
import domain.settings.model.ReviewSettings
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.GetReviewSettingsUseCase
import domain.streak.usecase.RecordStreakActivityUseCase
import domain.tts.model.TtsSettings
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class ReviewWordUseCases(
    val getDueWords: GetDueWordsUseCase,
    val getWordsByStage: GetWordsByStageUseCase,
    val reviewWord: ReviewWordUseCase,
    val updateWord: UpdateWordUseCase,
    val deleteWord: DeleteWordUseCase,
)

data class ReviewSessionUseCases(
    val startSession: StartStudySessionUseCase,
    val endSession: EndStudySessionUseCase,
    val recordEvent: RecordReviewEventUseCase,
    val recordStreak: RecordStreakActivityUseCase,
    val getSettings: GetReviewSettingsUseCase,
)

data class ReviewState(
    val review: ReviewScreenState = ReviewScreenState(),
    val ttsState: TtsState = TtsState.Idle,
    val speechRate: Float = TtsSettings.DEFAULT_SPEECH_RATE,
)

sealed class ReviewEffect {
    data class StartReview(val firstWord: Word) : ReviewEffect()
}

class ReviewViewModel(
    private val wordUseCases: ReviewWordUseCases,
    private val sessionUseCases: ReviewSessionUseCases,
    private val speakWordUseCase: SpeakWordUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val settingsRepository: ISettingsRepository,
    ttsRepository: ITtsRepository,
) : BaseViewModel<ReviewState, ReviewEffect>() {

    override fun initialState() = ReviewState()

    private var currentSessionId: String? = null
    private var sessionStartTime: Long = 0L
    private var cardShownTimestamp: Long = 0L
    private var reviewedCardCount: Int = 0
    private var correctCardCount: Int = 0
    private var incorrectCardCount: Int = 0
    private var sessionSettings: ReviewSettings = ReviewSettings.BALANCED

    init {
        viewModelScope.launch {
            ttsRepository.ttsState.collect { state ->
                updateState { copy(ttsState = state) }
            }
        }
        settingsRepository.getTtsSettings()
            .onEach { settings -> updateState { copy(speechRate = settings.speechRate) } }
            .launchIn(viewModelScope)
    }

    fun setTtsSpeechRate(rate: Float) {
        viewModelScope.launch { settingsRepository.setTtsSpeechRate(rate) }
    }

    fun startReview() {
        viewModelScope.launch {
            wordUseCases.getDueWords()
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
            wordUseCases.getDueWords()
                .map<List<Word>, UiState<List<Word>>> { UiState.Loaded(it) }
                .catch { e -> emit(UiState.Error(e.message ?: "Failed to load words")) }
                .first()
                .let { state ->
                    updateState { copy(review = review.copy(wordListState = state)) }
                    cardShownTimestamp = Clock.System.now().toEpochMilliseconds()
                }
        }
    }

    fun loadWordsByStage(stage: LearningStage) {
        viewModelScope.launch {
            updateState { copy(review = review.copy(wordListState = UiState.Loading)) }
            wordUseCases.getWordsByStage(stage)
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
        viewModelScope.launch {
            beginAnalyticsSession("BROWSE")
            cardShownTimestamp = Clock.System.now().toEpochMilliseconds()
            val wordListState = currentState.review.wordListState
            val cardCount = if (wordListState is UiState.Loaded) wordListState.value.size else 0
            analyticsTracker.logReviewSessionStart(cardCount = cardCount)
        }
    }

    fun reviewWord(word: Word, quality: Int) {
        viewModelScope.launch {
            val previousLevel = word.level
            val responseTime = Clock.System.now().toEpochMilliseconds() - cardShownTimestamp
            wordUseCases.reviewWord(word, quality)

            val wasCorrect = quality >= 1
            if (wasCorrect) correctCardCount++ else incorrectCardCount++
            reviewedCardCount++

            val newLevel = computeNewLevel(previousLevel, quality, sessionSettings)

            if (newLevel == 6 && previousLevel < 6) {
                analyticsTracker.logWordMastered(level = 6)
            }

            currentSessionId?.let { sid ->
                sessionUseCases.recordEvent(
                    ReviewEventParams(
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

            cardShownTimestamp = Clock.System.now().toEpochMilliseconds()

            analyticsTracker.logWordReviewed(
                rating = quality,
                wordLevel = word.level,
                wasCorrect = wasCorrect
            )
        }
    }

    fun updateWord(word: Word) {
        viewModelScope.launch {
            wordUseCases.updateWord(word).onSuccess { updatedWord ->
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
            wordUseCases.deleteWord(wordId).onSuccess {
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

    fun onReviewSessionComplete() {
        val wordListState = currentState.review.wordListState
        val count = if (wordListState is UiState.Loaded) wordListState.value.size else 0
        viewModelScope.launch {
            endAnalyticsSession(completedNormally = true)
            if (count > 0) {
                sessionUseCases.recordStreak(count)
                    .onSuccess { streakData ->
                        logNetwork("RecordActivity", "Success, count=$count")
                        analyticsTracker.logStreakUpdated(days = streakData.currentStreak, isNewRecord = false)
                    }
                    .onFailure { logNetwork("RecordActivity", "Failed, count=$count") }
            }
            updateState { copy(review = ReviewScreenState()) }
        }
    }

    fun speakWord(text: String, languageCode: String) {
        viewModelScope.launch {
            val resolvedCode = resolveLanguageCode(text, languageCode, currentState)
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
        if (currentSessionId != null) {
            viewModelScope.launch { endAnalyticsSession(completedNormally = false) }
        }
    }

    private suspend fun beginAnalyticsSession(reviewType: String) {
        val sessionId = Clock.System.now().toEpochMilliseconds().toString() +
            "-" + (0..999999).random().toString().padStart(6, '0')
        val startTime = Clock.System.now().toEpochMilliseconds()
        sessionSettings = sessionUseCases.getSettings(Unit)
            .fold(onSuccess = { it }, onFailure = { ReviewSettings.BALANCED })
        sessionUseCases.startSession(StartStudySessionUseCase.Params(sessionId, reviewType))
        // Set currentSessionId only after the session is registered in the recorder,
        // so that recordReviewEvent calls never target an unregistered session.
        currentSessionId = sessionId
        sessionStartTime = startTime
        reviewedCardCount = 0
        correctCardCount = 0
        incorrectCardCount = 0
    }

    private suspend fun endAnalyticsSession(completedNormally: Boolean) {
        val sid = currentSessionId ?: return
        val now = Clock.System.now().toEpochMilliseconds()
        sessionUseCases.endSession(
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
        analyticsTracker.logReviewSessionComplete(
            cardsReviewed = reviewedCardCount,
            durationMs = now - sessionStartTime,
            perfectCount = correctCardCount,
        )
        currentSessionId = null
    }
}

private fun computeNewLevel(
    previousLevel: Int,
    quality: Int,
    settings: ReviewSettings,
): Int {
    return when {
        quality == 0 -> maxOf(0, previousLevel - settings.forgotPenalty)
        settings.successesToAdvance <= 1 && previousLevel < 6 -> minOf(6, previousLevel + 1)
        previousLevel < 6 -> previousLevel
        else -> previousLevel
    }
}

private fun resolveLanguageCode(text: String, languageCode: String, state: ReviewState): String {
    if (languageCode.isNotBlank()) return languageCode

    val words = (state.review.wordListState as? UiState.Loaded<List<Word>>)?.value
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
