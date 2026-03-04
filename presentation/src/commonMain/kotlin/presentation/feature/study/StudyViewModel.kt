@file:OptIn(ExperimentalTime::class)

package presentation.feature.study

import analytics.IAnalyticsTracker
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import presentation.model.ProgressScreenState
import presentation.model.ReviewScreenState
import presentation.model.UiState
import presentation.util.NotificationStringHelper
import kotlin.time.ExperimentalTime

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
    getFeatureAccessUseCase: GetFeatureAccessUseCase,
    ttsRepository: ITtsRepository
) : ViewModel() {

    private val _progressStatistics = MutableStateFlow<ProgressScreenState?>(null)
    private var progressObservationJob: Job? = null

    // Consolidated Progress Screen State wrapped in UiState
    private val _progressScreenState = MutableStateFlow<UiState<ProgressScreenState>>(UiState.Loading)
    val progressScreenState: StateFlow<UiState<ProgressScreenState>> = _progressScreenState.asStateFlow()

    // Review Screen State for review bottom sheet
    private val _reviewScreenState = MutableStateFlow(ReviewScreenState())
    val reviewScreenState: StateFlow<ReviewScreenState> = _reviewScreenState.asStateFlow()

    val hasPremiumAccess: StateFlow<Boolean> = getFeatureAccessUseCase()
        .map { it.userAccess.hasPremiumAccess }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _events = Channel<StudyEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeAndCombineStates()
        startObservingProgress()
    }

    fun refreshStats() {
        progressObservationJob?.cancel()
        startObservingProgress()
    }

    private fun startObservingProgress() {
        progressObservationJob = viewModelScope.launch {
            getProgressStatsUseCase.invoke()
                .collect { stats ->
                    val screenState = ProgressScreenState(
                        progressStats = stats,
                        progressEvaluation = evaluateProgressUseCase(stats).getOrThrow(),
                        messageState = null
                    )
                    _progressStatistics.value = screenState

                    // Update analytics when stats change
                    analyticsTracker.updateUserProgress(
                        totalWords = stats.totalWords,
                        matureWords = stats.matureWords,
                        currentStreak = 0
                    )

                    // Reschedule notifications when stats change
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

    private fun observeAndCombineStates() {
        viewModelScope.launch {
            _progressStatistics.collect { state ->
                _progressScreenState.value = if (state != null) {
                    UiState.Loaded(state)
                } else {
                    UiState.Loading
                }
            }
        }
    }

    fun startReview() {
        viewModelScope.launch {
            getDueWordsUseCase()
                .catch { /* review unavailable */ }
                .firstOrNull()
                ?.firstOrNull()
                ?.let { _events.send(StudyEvent.StartReview(it)) }
        }
    }

    // === Review Functionality ===

    fun startDueReview() {
        viewModelScope.launch {
            _reviewScreenState.value = _reviewScreenState.value.copy(wordListState = UiState.Loading)
            getDueWordsUseCase()
                .map<List<Word>, UiState<List<Word>>> { UiState.Loaded(it) }
                .catch { e -> emit(UiState.Error(e.message ?: "Failed to load words")) }
                .first()
                .let { state ->
                    _reviewScreenState.value = _reviewScreenState.value.copy(wordListState = state)
                }
        }
    }

    fun loadWordsByStage(stage: LearningStage) {
        viewModelScope.launch {
            _reviewScreenState.value = _reviewScreenState.value.copy(wordListState = UiState.Loading)
            getWordsByStageUseCase(stage)
                .map<List<Word>, UiState<List<Word>>> { UiState.Loaded(it) }
                .catch { e -> emit(UiState.Error(e.message ?: "Failed to load words")) }
                .first()
                .let { state ->
                    _reviewScreenState.value = _reviewScreenState.value.copy(wordListState = state)
                }
        }
    }

    fun startStageReview(stage: LearningStage) {
        loadWordsByStage(stage)
        val currentState = _reviewScreenState.value.wordListState
        val cardCount = if (currentState is UiState.Loaded) currentState.value.size else 0
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
                val currentState = _reviewScreenState.value.wordListState
                if (currentState is UiState.Loaded) {
                    val updatedWords = currentState.value.map {
                        if (it.id == word.id) updatedWord else it
                    }
                    _reviewScreenState.value = _reviewScreenState.value.copy(
                        wordListState = UiState.Loaded(updatedWords)
                    )
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
                val currentState = _reviewScreenState.value.wordListState
                if (currentState is UiState.Loaded) {
                    val updatedWords = currentState.value.filterNot { it.id == wordId }
                    _reviewScreenState.value = _reviewScreenState.value.copy(
                        wordListState = UiState.Loaded(updatedWords)
                    )
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
        val wordListState = _reviewScreenState.value.wordListState
        val count = if (wordListState is UiState.Loaded) wordListState.value.size else 0
        viewModelScope.launch {
            if (count > 0) recordActivity(count)
            _reviewScreenState.value = ReviewScreenState()
        }
    }

    private suspend fun recordActivity(count: Int) {
        recordStreakActivityUseCase(count)
            .onSuccess { logNetwork("RecordActivity", "Success, count=$count") }
            .onFailure { logNetwork("RecordActivity", "Failed, count=$count") }
    }

    // === TTS Functionality ===

    val ttsState: StateFlow<TtsState> = ttsRepository.ttsState

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

        val words = (_reviewScreenState.value.wordListState as? UiState.Loaded<List<Word>>)?.value
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
