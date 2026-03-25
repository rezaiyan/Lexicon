package feature.study

import domain.tts.model.TtsSettings
import domain.tts.model.TtsState
import domain.word.model.Word
import domain.word.model.ReviewSource
import feature.study.model.ReviewError
import feature.study.model.ReviewType

data class ReviewViewModelState(
    val review: ReviewState = ReviewState.Idle,
    val ttsState: TtsState = TtsState.Idle,
    val speechRate: Float = TtsSettings.DEFAULT_SPEECH_RATE,
)

sealed class ReviewState {
    data object Idle : ReviewState()
    data object Loading : ReviewState()
    data class Error(val error: ReviewError, val source: ReviewSource) : ReviewState()
    data object Empty : ReviewState()

    data class Active(
        val words: List<Word>,
        val currentIndex: Int = 0,
        val isFlipped: Boolean = false,
        val reviewType: ReviewType,
        val knownCount: Int = 0,
        val unknownCount: Int = 0,
        val isAutoPlayEnabled: Boolean = false,
    ) : ReviewState() {
        val currentWord: Word get() = words[currentIndex]
        val isLastCard: Boolean get() = currentIndex == words.size - 1
        val progress: Int get() = currentIndex + 1
        val total: Int get() = words.size
    }

    data class Completed(val knownCount: Int, val unknownCount: Int) : ReviewState()
}

sealed class ReviewEffect {
    data object SessionComplete : ReviewEffect()
}
