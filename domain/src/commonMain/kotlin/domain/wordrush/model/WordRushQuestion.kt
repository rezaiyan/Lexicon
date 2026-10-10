package domain.wordrush.model

import domain.word.model.Word

/** Which side of the card is shown as the prompt. */
enum class WordRushDirection {
    /** Show the term in the learning language, pick its meaning. */
    Recognize,

    /** Show the meaning, pick the term in the learning language. */
    Recall,
}

data class WordRushQuestion(
    val word: Word,
    val direction: WordRushDirection,
    val options: List<String>,
    val correctIndex: Int,
) {
    val prompt: String
        get() = when (direction) {
            WordRushDirection.Recognize -> word.originalWord
            WordRushDirection.Recall -> word.translation
        }

    val answer: String
        get() = options[correctIndex]
}

/** A question the player got wrong or ran out of time on. [chosenAnswer] is null on time-out. */
data class WordRushMiss(
    val prompt: String,
    val correctAnswer: String,
    val chosenAnswer: String?,
)
