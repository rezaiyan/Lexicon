@file:OptIn(ExperimentalTime::class)

package presentation.feature.study

import analytics.IAnalyticsTracker
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.common.onFailure
import domain.common.onSuccess
import domain.notifications.usecase.ScheduleNotificationsUseCase
import domain.streak.usecase.RecordStreakActivityUseCase
import domain.tts.model.TtsState
import domain.tts.repository.ITtsRepository
import domain.tts.usecase.SpeakWordUseCase
import domain.tts.usecase.StopSpeakingUseCase
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.usecase.DeleteWordUseCase
import domain.word.usecase.GetDueWordsUseCase
import domain.word.usecase.GetProgressStatsUseCase
import domain.word.usecase.GetWordsByStageUseCase
import domain.word.usecase.ReviewWordUseCase
import domain.word.usecase.UpdateWordUseCase
import expects.logNetwork
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import presentation.model.MessageState
import presentation.model.ProgressScreenState
import presentation.model.ReviewScreenState
import presentation.model.UiState
import presentation.util.NotificationStringHelper
import kotlin.time.ExperimentalTime

class StudyViewModel(
    private val getProgressStatsUseCase: GetProgressStatsUseCase,
    private val scheduleNotificationsUseCase: ScheduleNotificationsUseCase,
    private val getDueWordsUseCase: GetDueWordsUseCase,
    private val getWordsByStageUseCase: GetWordsByStageUseCase,
    private val reviewWordUseCase: ReviewWordUseCase,
    private val updateWordUseCase: UpdateWordUseCase,
    private val deleteWordUseCase: DeleteWordUseCase,
    private val recordStreakActivityUseCase: RecordStreakActivityUseCase,
    private val speakWordUseCase: SpeakWordUseCase,
    private val stopSpeakingUseCase: StopSpeakingUseCase,
    private val ttsRepository: ITtsRepository,
    private val analyticsTracker: IAnalyticsTracker
) : ViewModel() {

    private val _progressStatistics = MutableStateFlow<ProgressStats?>(null)
    private var progressObservationJob: Job? = null

    // Notification settings (passed in for scheduling)
    private val notificationsEnabled = true
    private val systemNotificationsEnabled = true

    // Consolidated Progress Screen State wrapped in UiState
    private val _progressScreenState = MutableStateFlow<UiState<ProgressScreenState>>(UiState.Loading)
    val progressScreenState: StateFlow<UiState<ProgressScreenState>> = _progressScreenState.asStateFlow()

    // Review Screen State for review bottom sheet
    private val _reviewScreenState = MutableStateFlow(ReviewScreenState())
    val reviewScreenState: StateFlow<ReviewScreenState> = _reviewScreenState.asStateFlow()

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
                    _progressStatistics.value = stats

                    // Update analytics when stats change
                    analyticsTracker.updateUserProgress(
                        totalWords = stats.totalWords,
                        matureWords = stats.matureWords,
                        currentStreak = 0
                    )

                    // Reschedule notifications when stats change
                    if (notificationsEnabled && systemNotificationsEnabled) {
                        val notifStrings =
                            NotificationStringHelper.getNotificationResources(
                                stats.dueCards
                            )
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
    }

    private fun observeAndCombineStates() {
        viewModelScope.launch {
            _progressStatistics.collect { stats ->
                if (stats != null) {
                    _progressScreenState.value = UiState.Loaded(
                        ProgressScreenState(
                            progressStats = stats,
                            messageState = null
                        )
                    )
                } else {
                    _progressScreenState.value = UiState.Loading
                }
            }
        }
    }

    fun startReview() {
        viewModelScope.launch {
            try {
                val words = getDueWordsUseCase().first()
                val firstWord = words.firstOrNull()
                if (firstWord != null) {
                    _events.send(StudyEvent.StartReview(firstWord))
                }
            } catch (_: Exception) {
                // Error loading due words for review
            }
        }
    }

    // === Review Functionality (merged from VocabularyViewModel) ===

    fun startDueReview() {
        viewModelScope.launch {
            _reviewScreenState.value = _reviewScreenState.value.copy(wordListState = UiState.Loading)
            try {
                val words = getDueWordsUseCase().first()
                _reviewScreenState.value = _reviewScreenState.value.copy(
                    wordListState = UiState.Loaded(words)
                )
            } catch (e: Exception) {
                _reviewScreenState.value = _reviewScreenState.value.copy(
                    wordListState = UiState.Error(e.message ?: "Failed to load words")
                )
            }
        }
    }

    fun loadWordsByStage(stage: LearningStage) {
        viewModelScope.launch {
            _reviewScreenState.value = _reviewScreenState.value.copy(wordListState = UiState.Loading)
            try {
                val words = getWordsByStageUseCase(stage).first()
                _reviewScreenState.value = _reviewScreenState.value.copy(
                    wordListState = UiState.Loaded(words)
                )
            } catch (e: Exception) {
                _reviewScreenState.value = _reviewScreenState.value.copy(
                    wordListState = UiState.Error(e.message ?: "Failed to load words")
                )
            }
        }
    }

    fun startStageReview(stage: LearningStage) {
        loadWordsByStage(stage)
        onRecordActivity()
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
                    val updatedWords = currentState.value.filterNot { it.id.toInt() == wordId }
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
        // Refresh current review state by reloading due words
        startDueReview()
    }

    fun onReviewSessionComplete() {
        viewModelScope.launch {
            recordStreakActivityUseCase()
                .onSuccess {
                    logNetwork("RecordActivity", "Success")
                }.onFailure {
                    logNetwork("RecordActivity", "Failed")
                }
            _reviewScreenState.value = ReviewScreenState() // Reset
        }
    }

    private fun onRecordActivity() {
        viewModelScope.launch {
            recordStreakActivityUseCase()
                .onSuccess {
                    logNetwork("RecordActivity", "Success")
                }.onFailure {
                    logNetwork("RecordActivity", "Failed")
                }
        }
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

        // Determine if text is on the target side (originalWord) or source side (translation)
        val isTargetSide = words.any { it.originalWord == text }

        // Derive the most common language code from words
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

    fun stopSpeaking() {
        viewModelScope.launch {
            stopSpeakingUseCase()
        }
    }
}

