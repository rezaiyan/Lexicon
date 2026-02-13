@file:OptIn(kotlin.time.ExperimentalTime::class)

package presentation.viewmodel

import analytics.IAnalyticsTracker
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.streak.repository.IStreakRepository
import domain.word.model.LearningStage
import domain.word.model.Word
import domain.word.usecase.DeleteWordUseCase
import domain.word.usecase.GetDueWordsUseCase
import domain.word.usecase.GetWordsByStageUseCase
import domain.word.usecase.ReviewWordUseCase
import domain.word.usecase.UpdateWordUseCase
import events.VocabularyEffect
import expects.logNetwork
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import presentation.model.ReviewMode
import presentation.model.ReviewScreenState
import presentation.model.UiMessage
import presentation.model.UiState

class VocabularyViewModel(
    private val getDueWordsUseCase: GetDueWordsUseCase,
    private val getWordsByStageUseCase: GetWordsByStageUseCase,
    private val reviewWordUseCase: ReviewWordUseCase,
    private val updateWordUseCase: UpdateWordUseCase,
    private val deleteWordUseCase: DeleteWordUseCase,
    private val streakRepository: IStreakRepository,
    private val analyticsTracker: IAnalyticsTracker,
) : ViewModel() {

    private val _events = Channel<VocabularyEffect>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _uiMessages = Channel<UiMessage>(Channel.BUFFERED)
    val uiMessages = _uiMessages.receiveAsFlow()

    private val _wordListState = MutableStateFlow<UiState<List<Word>>>(UiState.Loading)

    private val _reviewScreenState = MutableStateFlow(ReviewScreenState())
    val reviewScreenState: StateFlow<ReviewScreenState> = _reviewScreenState.asStateFlow()

    private var currentReviewMode: ReviewMode = ReviewMode.DuoCards

    init {
        observeAndCombineStates()
    }

    private fun observeAndCombineStates() {
        viewModelScope.launch {
            _wordListState.collect { wordList ->
                _reviewScreenState.value = _reviewScreenState.value.copy(
                    wordListState = wordList
                )
            }
        }
    }

    fun loadWords(reviewMode: ReviewMode = ReviewMode.DuoCards) {
        viewModelScope.launch {
            currentReviewMode = reviewMode
            _wordListState.value = UiState.Loading

            try {
                val words = when (reviewMode) {
                    is ReviewMode.DuoCards -> getDueWordsUseCase()
                    is ReviewMode.ByStage -> getWordsByStageUseCase(reviewMode.stage)
                }.first()
                _wordListState.value = UiState.Loaded(words)
            } catch (e: Exception) {
                _wordListState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadWordsByStage(stage: LearningStage) {
        loadWords(ReviewMode.ByStage(stage))
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

    fun onReviewSessionComplete() {
        viewModelScope.launch {
            _events.send(VocabularyEffect.ReviewSessionComplete)
            _uiMessages.send(UiMessage.ReviewComplete)
        }
    }

    fun startDueReview() {
        loadWords()
        analyticsTracker.logReviewSessionStart(cardCount = 0)
    }

    fun startStageReview(stage: LearningStage) {
        loadWordsByStage(stage)
        onRecordActivity()

        val cardCount = when (val state = _wordListState.value) {
            is UiState.Loaded -> state.value.size
            else -> 0
        }
        analyticsTracker.logReviewSessionStart(cardCount = cardCount)
    }

    fun onRecordActivity() {
        viewModelScope.launch {
            streakRepository.recordActivity()
                .onSuccess {
                    logNetwork("RecordActivity", "Success")
                }.onFailure {
                    logNetwork("RecordActivity", "Failed")
                }
        }
    }

    fun updateWord(word: Word) {
        viewModelScope.launch {
            val result = updateWordUseCase(word)
            result.fold(
                onSuccess = {
                    loadWords(currentReviewMode)
                    analyticsTracker.logEvent("word_updated_in_review")
                },
                onFailure = { error ->
                    analyticsTracker.logNonFatalError(
                        message = "Word update failed in review",
                        additionalInfo = mapOf("error" to (error.message ?: "unknown"))
                    )
                }
            )
        }
    }

    fun deleteWord(wordId: Int, onDeleted: () -> Unit) {
        viewModelScope.launch {
            val result = deleteWordUseCase(wordId)
            result.fold(
                onSuccess = {
                    _uiMessages.send(UiMessage.WordDeleted)
                    loadWords(currentReviewMode)
                    onDeleted()
                    analyticsTracker.logEvent("word_deleted_in_review")
                },
                onFailure = { error ->
                    analyticsTracker.logNonFatalError(
                        message = "Word deletion failed in review",
                        additionalInfo = mapOf("error" to (error.message ?: "unknown"))
                    )
                }
            )
        }
    }

    fun onEvent(event: VocabularyEvent) {
        when (event) {
            is VocabularyEvent.LoadWords -> loadWords(event.reviewMode)
            is VocabularyEvent.LoadWordsByStage -> loadWordsByStage(event.stage)
            is VocabularyEvent.ReviewWord -> reviewWord(event.word, event.quality)
            is VocabularyEvent.UpdateWord -> updateWord(event.word)
            is VocabularyEvent.DeleteWord -> deleteWord(event.wordId, event.onDeleted)
            is VocabularyEvent.StartDueReview -> startDueReview()
            is VocabularyEvent.StartStageReview -> startStageReview(event.stage)
            is VocabularyEvent.RecordActivity -> onRecordActivity()
            is VocabularyEvent.ReviewSessionComplete -> onReviewSessionComplete()
        }
    }
}
