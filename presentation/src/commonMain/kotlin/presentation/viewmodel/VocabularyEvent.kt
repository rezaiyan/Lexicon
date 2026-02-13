package presentation.viewmodel

import domain.word.model.LearningStage
import domain.word.model.Word
import presentation.model.ReviewMode

sealed interface VocabularyEvent {
    data class LoadWords(val reviewMode: ReviewMode = ReviewMode.DuoCards) : VocabularyEvent
    data class LoadWordsByStage(val stage: LearningStage) : VocabularyEvent
    data class ReviewWord(val word: Word, val quality: Int) : VocabularyEvent
    data class UpdateWord(val word: Word) : VocabularyEvent
    data class DeleteWord(val wordId: Int, val onDeleted: () -> Unit) : VocabularyEvent
    data object StartDueReview : VocabularyEvent
    data class StartStageReview(val stage: LearningStage) : VocabularyEvent
    data object RecordActivity : VocabularyEvent
    data object ReviewSessionComplete : VocabularyEvent
}
