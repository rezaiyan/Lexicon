package feature.study

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.onFailure
import core.common.onSuccess
import domain.analytics.model.ReviewEventParams
import domain.analytics.usecase.EndStudySessionUseCase
import domain.analytics.usecase.RecordReviewEventUseCase
import domain.analytics.usecase.StartStudySessionUseCase
import domain.settings.usecase.ObserveSpeechRateUseCase
import domain.settings.usecase.SetTtsSpeechRateUseCase
import domain.streak.usecase.RecordStreakActivityUseCase
import domain.tts.usecase.ObserveTtsStateUseCase
import domain.tts.usecase.SpeakWordUseCase
import domain.word.model.ReviewSource
import domain.word.model.ReviewWordResult
import domain.word.model.Word
import domain.word.usecase.DeleteWordUseCase
import domain.word.usecase.LoadReviewQueueUseCase
import domain.word.usecase.ReviewWordUseCase
import domain.word.usecase.UpdateWordUseCase
import feature.study.model.ReviewError
import feature.study.model.ReviewType
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Clock

@Suppress(
    "TooManyFunctions",   // each public method is a distinct event-sink entry point required by the screen
    "LongParameterList",  // all parameters are mandatory use-case dependencies injected by Koin
)
class ReviewViewModel(
    private val loadQueueUseCase: LoadReviewQueueUseCase,
    private val reviewWordUseCase: ReviewWordUseCase,
    private val updateWordUseCase: UpdateWordUseCase,
    private val deleteWordUseCase: DeleteWordUseCase,
    private val startSessionUseCase: StartStudySessionUseCase,
    private val endSessionUseCase: EndStudySessionUseCase,
    private val recordEventUseCase: RecordReviewEventUseCase,
    private val recordStreakUseCase: RecordStreakActivityUseCase,
    private val speakWordUseCase: SpeakWordUseCase,
    private val observeTtsState: ObserveTtsStateUseCase,
    private val observeSpeechRate: ObserveSpeechRateUseCase,
    private val setSpeechRateUseCase: SetTtsSpeechRateUseCase,
    private val analyticsTracker: IAnalyticsTracker,
) : BaseViewModel<ReviewViewModelState, ReviewEffect>() {

    override fun initialState() = ReviewViewModelState()

    // ---------------------------------------------------------------------------
    // Private session state — immutable copy pattern, not a mutable manager
    // ---------------------------------------------------------------------------

    private data class SessionContext(
        val sessionId: String,
        val reviewType: String,
        val startedAt: Long,
        val reviewed: Int = 0,
        val correct: Int = 0,
        val incorrect: Int = 0,
    ) {
        fun withReview(wasCorrect: Boolean) = copy(
            reviewed = reviewed + 1,
            correct = if (wasCorrect) correct + 1 else correct,
            incorrect = if (!wasCorrect) incorrect + 1 else incorrect,
        )

        fun toEndParams(now: Long, completedNormally: Boolean) = EndStudySessionUseCase.Params(
            sessionId = sessionId,
            endedAt = now,
            durationMs = now - startedAt,
            totalCards = reviewed,
            correctCount = correct,
            incorrectCount = incorrect,
            completedNormally = completedNormally,
        )
    }

    private var sessionContext: SessionContext? = null
    private var cardShownTimestamp: Long = 0L

    init {
        observeTtsState(Unit)
            .onEach { state -> updateState { copy(ttsState = state) } }
            .catch { }
            .launchIn(viewModelScope)

        observeSpeechRate(Unit)
            .onEach { rate -> updateState { copy(speechRate = rate) } }
            .catch { }
            .launchIn(viewModelScope)
    }

    // ---------------------------------------------------------------------------
    // Public event-sink API
    // ---------------------------------------------------------------------------

    fun startSession(source: ReviewSource) {
        viewModelScope.launch {
            updateState { copy(review = ReviewState.Loading) }
            loadQueueUseCase(source)
                .onSuccess { words ->
                    if (words.isEmpty()) {
                        updateState { copy(review = ReviewState.Empty) }
                        return@onSuccess
                    }
                    val startedAt = Clock.System.now().toEpochMilliseconds()
                    val sessionId = buildSessionId()
                    val sessionType = source.toSessionType()
                    sessionContext = SessionContext(sessionId, sessionType, startedAt)
                    startSessionUseCase(StartStudySessionUseCase.Params(sessionId, sessionType))
                    analyticsTracker.logReviewSessionStart(cardCount = words.size)
                    updateState {
                        copy(review = ReviewState.Active(words = words, reviewType = source.toReviewType()))
                    }
                    cardShownTimestamp = Clock.System.now().toEpochMilliseconds()
                    autoPlayIfEnabled()
                }
                .onFailure { error ->
                    updateState { copy(review = ReviewState.Error(error.toReviewError(), source)) }
                }
        }
    }

    fun reviewWord(quality: Int) {
        val active = currentState.review as? ReviewState.Active ?: return
        val responseTime = Clock.System.now().toEpochMilliseconds() - cardShownTimestamp
        val wasCorrect = quality >= 1
        viewModelScope.launch {
            reviewWordUseCase(active.currentWord, quality)
                .onSuccess { result ->
                    val newKnown = if (wasCorrect) active.knownCount + 1 else active.knownCount
                    val newUnknown = if (!wasCorrect) active.unknownCount + 1 else active.unknownCount
                    sessionContext = sessionContext?.withReview(wasCorrect)
                    buildEventParams(result, quality, responseTime)?.let { recordEventUseCase(it) }
                    if (result.wasMastered) analyticsTracker.logWordMastered(level = 6)
                    analyticsTracker.logWordReviewed(
                        rating = quality,
                        wordLevel = result.previousLevel,
                        wasCorrect = wasCorrect,
                    )
                    val updatedWords = active.words.map {
                        if (it.id == result.updatedWord.id) result.updatedWord else it
                    }
                    updateState {
                        copy(
                            review = active.copy(
                                words = updatedWords,
                                knownCount = newKnown,
                                unknownCount = newUnknown,
                            )
                        )
                    }
                    advanceOrComplete(newKnown, newUnknown)
                }
                .onFailure { error ->
                    analyticsTracker.logNonFatalError(
                        message = "Word review failed",
                        additionalInfo = mapOf("error" to (error.message ?: "unknown")),
                    )
                    advanceOrComplete(active.knownCount, active.unknownCount)
                }
        }
    }

    fun flipCard() {
        updateActiveState { copy(isFlipped = !isFlipped) }
    }

    fun navigateBack() {
        updateActiveState {
            if (currentIndex > 0) copy(currentIndex = currentIndex - 1, isFlipped = false) else this
        }
    }

    fun navigateForward() {
        updateActiveState {
            if (currentIndex < words.size - 1) copy(currentIndex = currentIndex + 1, isFlipped = false) else this
        }
    }

    fun updateWord(word: Word) {
        viewModelScope.launch {
            updateWordUseCase(word)
                .onSuccess { updatedWord ->
                    updateActiveState {
                        copy(words = words.map { if (it.id == updatedWord.id) updatedWord else it })
                    }
                    analyticsTracker.logEvent("word_updated_in_review")
                }
                .onFailure { error ->
                    analyticsTracker.logNonFatalError(
                        message = "Word update failed in review",
                        additionalInfo = mapOf("error" to (error.message ?: "unknown")),
                    )
                }
        }
    }

    fun deleteWord(wordId: Int) {
        viewModelScope.launch {
            deleteWordUseCase(wordId)
                .onSuccess {
                    val active = currentState.review as? ReviewState.Active ?: return@onSuccess
                    val updatedWords = active.words.filterNot { it.id == wordId }
                    if (updatedWords.isEmpty()) {
                        completeSession(active.knownCount, active.unknownCount)
                    } else {
                        val safeIndex = active.currentIndex.coerceAtMost(updatedWords.size - 1)
                        updateState {
                            copy(
                                review = active.copy(
                                    words = updatedWords,
                                    currentIndex = safeIndex,
                                    isFlipped = false,
                                )
                            )
                        }
                    }
                    analyticsTracker.logEvent("word_deleted_in_review")
                }
                .onFailure { error ->
                    analyticsTracker.logNonFatalError(
                        message = "Word deletion failed in review",
                        additionalInfo = mapOf("error" to (error.message ?: "unknown")),
                    )
                }
        }
    }

    fun speakWord(text: String, languageCode: String) {
        viewModelScope.launch {
            speakWordUseCase(text, resolveLanguageCode(text, languageCode))
        }
    }

    fun setAutoPlay(enabled: Boolean) {
        updateActiveState { copy(isAutoPlayEnabled = enabled) }
        if (enabled) autoPlayIfEnabled()
    }

    fun setTtsSpeechRate(rate: Float) {
        viewModelScope.launch { setSpeechRateUseCase(rate) }
    }

    fun abandonSession() {
        val ctx = sessionContext ?: return
        viewModelScope.launch { endSessionInternal(ctx, completedNormally = false) }
        updateState { copy(review = ReviewState.Idle) }
    }

    fun acknowledgeCompletion() {
        emitEffect(ReviewEffect.SessionComplete)
        updateState { copy(review = ReviewState.Idle) }
    }

    override fun onCleared() {
        super.onCleared()
        val ctx = sessionContext ?: return
        viewModelScope.launch(NonCancellable) { endSessionInternal(ctx, completedNormally = false) }
    }

    // ---------------------------------------------------------------------------
    // Private suspend helpers
    // ---------------------------------------------------------------------------

    private suspend fun advanceOrComplete(knownCount: Int, unknownCount: Int) {
        val active = currentState.review as? ReviewState.Active ?: return
        if (active.isLastCard) {
            completeSession(knownCount, unknownCount)
        } else {
            updateState {
                copy(review = active.copy(currentIndex = active.currentIndex + 1, isFlipped = false))
            }
            cardShownTimestamp = Clock.System.now().toEpochMilliseconds()
            autoPlayIfEnabled()
        }
    }

    private suspend fun completeSession(knownCount: Int, unknownCount: Int) {
        val ctx = sessionContext ?: return
        endSessionInternal(ctx, completedNormally = true)
        if (ctx.reviewed > 0) {
            recordStreakUseCase(ctx.reviewed)
                .onSuccess { analyticsTracker.logStreakUpdated(days = it.currentStreak, isNewRecord = false) }
        }
        updateState { copy(review = ReviewState.Completed(knownCount, unknownCount)) }
    }

    private suspend fun endSessionInternal(ctx: SessionContext, completedNormally: Boolean) {
        val now = Clock.System.now().toEpochMilliseconds()
        endSessionUseCase(ctx.toEndParams(now, completedNormally))
        analyticsTracker.logReviewSessionComplete(
            cardsReviewed = ctx.reviewed,
            durationMs = now - ctx.startedAt,
            perfectCount = ctx.correct,
        )
        sessionContext = null
    }

    // ---------------------------------------------------------------------------
    // Private non-suspend helpers
    // ---------------------------------------------------------------------------

    private fun updateActiveState(reducer: ReviewState.Active.() -> ReviewState.Active) {
        val active = currentState.review as? ReviewState.Active ?: return
        updateState { copy(review = active.reducer()) }
    }

    private fun autoPlayIfEnabled() {
        val active = currentState.review as? ReviewState.Active ?: return
        if (!active.isAutoPlayEnabled) return
        viewModelScope.launch {
            speakWordUseCase(active.currentWord.originalWord, active.currentWord.sourceLanguage.code)
        }
    }

    private fun buildSessionId(): String =
        "${Clock.System.now().toEpochMilliseconds()}-${(0..999999).random().toString().padStart(6, '0')}"

    private fun buildEventParams(
        result: ReviewWordResult,
        quality: Int,
        responseTimeMs: Long,
    ): ReviewEventParams? {
        val ctx = sessionContext ?: return null
        return ReviewEventParams(
            sessionId = ctx.sessionId,
            wordId = result.updatedWord.id,
            wordText = result.updatedWord.originalWord,
            wordTranslation = result.updatedWord.translation,
            sourceLanguage = result.updatedWord.sourceLanguage.code,
            targetLanguage = result.updatedWord.targetLanguage.code,
            rating = quality,
            previousLevel = result.previousLevel,
            newLevel = result.newLevel,
            responseTimeMs = responseTimeMs,
            reviewedAt = Clock.System.now().toEpochMilliseconds(),
        )
    }

    private fun resolveLanguageCode(text: String, languageCode: String): String {
        if (languageCode.isNotBlank()) return languageCode
        val words = (currentState.review as? ReviewState.Active)?.words ?: return languageCode
        val isTargetSide = words.any { it.originalWord == text }
        return words
            .map { if (isTargetSide) it.targetLanguage.code else it.sourceLanguage.code }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: languageCode
    }

    private fun ReviewSource.toReviewType() = when (this) {
        is ReviewSource.DueCards, is ReviewSource.ByTag -> ReviewType.REVIEW
        is ReviewSource.ByStage, is ReviewSource.ByStageAndTag -> ReviewType.BROWSE
    }

    private fun ReviewSource.toSessionType() = when (this) {
        is ReviewSource.DueCards, is ReviewSource.ByTag -> "REVIEW"
        is ReviewSource.ByStage, is ReviewSource.ByStageAndTag -> "BROWSE"
    }

    private fun Throwable.toReviewError(): ReviewError = when {
        message?.contains("timeout", ignoreCase = true) == true ||
        message?.contains("connect", ignoreCase = true) == true ||
        message?.contains("network", ignoreCase = true) == true -> ReviewError.Network
        else -> ReviewError.Unknown(message ?: "Unknown error")
    }
}
