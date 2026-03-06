package feature.study

import domain.word.model.Word

sealed class StudyEvent {
    data class StartReview(val firstWord: Word) : StudyEvent()
}

