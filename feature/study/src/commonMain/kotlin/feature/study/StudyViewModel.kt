@file:OptIn(ExperimentalTime::class)

package feature.study

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import domain.auth.usecase.GetFeatureAccessUseCase
import core.common.getOrThrow
import core.common.onFailure
import core.common.onSuccess
import domain.notifications.usecase.ScheduleNotificationsUseCase
import domain.streak.usecase.RecordStreakActivityUseCase
import domain.tts.model.TtsState
import domain.tts.repository.ITtsRepository
import domain.tts.usecase.SpeakWordUseCase
import domain.word.model.LearningStage
import domain.word.model.Word
import domain.word.usecase.DeleteWordUseCase
import domain.word.usecase.EvaluateProgressUseCase
import domain.word.usecase.GetDueWordsUseCase
import domain.word.usecase.GetProgressStatsUseCase
import domain.word.usecase.GetWordsByStageUseCase
import domain.word.usecase.ReviewWordUseCase
import domain.word.usecase.UpdateWordUseCase
import expects.logNetwork
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import core.base.BaseViewModel
import feature.study.model.ProgressScreenState
import feature.study.model.ReviewScreenState
import core.common.UiState
import feature.study.util.NotificationStringHelper
import performance.IPerformanceTracer
import kotlin.time.ExperimentalTime

data class StudyScreenState(
    val progress: UiState<ProgressScreenState> = UiState.Loading,
    val review: ReviewScreenState = ReviewScreenState(),
    val hasPremiumAccess: Boolean = false,
    val ttsState: TtsState = TtsState.Idle,
)

class StudyViewModel(
    private val getProgressStatsUseCase: GetProgressStatsUseCase,
    private val evaluateProgressUseCase: EvaluateProgressUseCase,
    private val scheduleNotificationsUseCase: ScheduleNotificationsUseCase,
    private val getDueWordsUseCase: GetDueWordsUseCase,
    private val getWordsByStageUseCase: GetWordsByStageUseCase,
    private val reviewWordUseCase: ReviewWordUseCase,
    private val updateWordUseCase: UpdateWordUseCase,
    private val deleteWordUseCase: DeleteWordUseCase,
    private val recordStreakActivityUseCase: RecordStreakActivityUseCase,
    private val speakWordUseCase: SpeakWordUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val performanceTracer: IPerformanceTracer,
    getFeatureAccessUseCase: GetFeatureAccessUseCase,
    ttsRepository: ITtsRepository
) : BaseViewModel<StudyScreenState, StudyEvent>() {

    override fun initialState() = StudyScreenState()

    private var progressObservationJob: Job? = null

    init {
        observeFeatureAccess(getFeatureAccessUseCase)
        observeTtsState(ttsRepository)
        startObservingProgress()
    }

    private fun observeTtsState(ttsRepository: ITtsRepository) {
        viewModelScope.launch {
            ttsRepository.ttsState.collect { state ->
                updateState { copy(ttsState = state) }
            }
        }
    }

    private fun observeFeatureAccess(getFeatureAccessUseCase: GetFeatureAccessUseCase) {
        viewModelScope.launch {
            getFeatureAccessUseCase()
                .map { it.userAccess.hasPremiumAccess }
                .catch { emit(false) }
                .collect { hasPremium ->
                    updateState { copy(hasPremiumAccess = hasPremium) }
                }
        }
    }

    fun refreshStats() {
        progressObservationJob?.cancel()
        startObservingProgress()
    }

    private fun startObservingProgress() {
        progressObservationJob = viewModelScope.launch {
            val trace = performanceTracer.startTrace("study_session_load")
            getProgressStatsUseCase.invoke()
                .collect { stats ->
                    val screenState = ProgressScreenState(
                        progressStats = stats,
                        progressEvaluation = evaluateProgressUseCase(stats).getOrThrow(),
                        messageState = null
                    )
                    updateState { copy(progress = UiState.Loaded(screenState)) }
                    performanceTracer.putMetric(trace, "total_words", stats.totalWords.toLong())
                    performanceTracer.putMetric(trace, "due_cards", stats.dueCards.toLong())
                    performanceTracer.stopTrace(trace)

                    analyticsTracker.updateUserProgress(
                        totalWords = stats.totalWords,
                        matureWords = stats.matureWords,
                        currentStreak = 0
                    )

                    val notifStrings =
                        NotificationStringHelper.getNotificationResources(stats.dueCards)
                    val title = getString(
                        notifStrings.titleRes,
                        *notifStrings.titleParams.toTypedArray()
                    )
                    val message = getString(
                        notifStrings.messageRes,
                        *notifStrings.messageParams.toTypedArray()
                    )
                    scheduleNotificationsUseCase(
                        stats = stats,
                        titleProvider = { title },
                        messageProvider = { message }
                    )
                }
        }
    }

    fun startReview() {
        viewModelScope.launch {
            getDueWordsUseCase()
                .catch { /* review unavailable */ }
                .firstOrNull()
                ?.firstOrNull()
                ?.let { emitEffect(StudyEvent.StartReview(it)) }
        }
    }

    // === Review Functionality ===

    fun startDueReview() {
        viewModelScope.launch {
            updateState { copy(review = review.copy(wordListState = UiState.Loading)) }
            getDueWordsUseCase()
                .map<List<Word>, UiState<List<Word>>> { UiState.Loaded(it) }
                .catch { e -> emit(UiState.Error(e.message ?: "Failed to load words")) }
                .first()
                .let { state ->
                    updateState { copy(review = review.copy(wordListState = state)) }
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
        val wordListState = currentState.review.wordListState
        val cardCount = if (wordListState is UiState.Loaded) wordListState.value.size else 0
        analyticsTracker.logReviewSessionStart(cardCount = cardCount)
    }

    fun reviewWord(word: Word, quality: Int) {
        viewModelScope.launch {
            reviewWordUseCase(word, quality)
            analyticsTracker.logWordReviewed(
                rating = quality,
                wordLevel = word.level,
                wasCorrect = quality >= 1
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
            if (count > 0) recordActivity(count)
            updateState { copy(review = ReviewScreenState()) }
        }
    }

    private suspend fun recordActivity(count: Int) {
        recordStreakActivityUseCase(count)
            .onSuccess { logNetwork("RecordActivity", "Success, count=$count") }
            .onFailure { logNetwork("RecordActivity", "Failed, count=$count") }
    }

    // === TTS Functionality ===

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
