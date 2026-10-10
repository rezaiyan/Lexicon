package domain.wordrush

import domain.word.model.Word
import domain.wordrush.model.WordRushDirection
import domain.wordrush.model.WordRushQuestion
import kotlin.random.Random

/**
 * Turns the words picked for a round into multiple-choice questions.
 *
 * Distractors come from the whole [pool] (not just the round), prefer the same language pair
 * as the asked word, and never repeat or equal the correct answer — two words sharing a
 * translation must not produce two identical options where only one counts as correct.
 */
object WordRushRoundBuilder {

    const val OPTIONS_COUNT = 4

    /** Roughly one question in three asks for the term instead of its meaning. */
    private const val RECALL_ONE_IN = 3

    fun build(
        roundWords: List<Word>,
        pool: List<Word>,
        random: Random = Random.Default,
    ): List<WordRushQuestion> = roundWords.mapNotNull { word ->
        val preferred = when (random.nextInt(RECALL_ONE_IN)) {
            0 -> WordRushDirection.Recall
            else -> WordRushDirection.Recognize
        }
        buildQuestion(word, preferred, pool, random)
            ?: buildQuestion(word, preferred.flipped(), pool, random)
    }

    private fun buildQuestion(
        word: Word,
        direction: WordRushDirection,
        pool: List<Word>,
        random: Random,
    ): WordRushQuestion? {
        val answer = word.answerFor(direction)
        val taken = mutableSetOf(answer.normalized())
        val (sameLanguage, otherLanguage) = pool
            .filter { it.id != word.id }
            .partition { it.sourceLanguage == word.sourceLanguage && it.targetLanguage == word.targetLanguage }
        val distractors = (sameLanguage.shuffled(random) + otherLanguage.shuffled(random))
            .map { it.answerFor(direction) }
            .filter { it.isNotBlank() && taken.add(it.normalized()) }
            .take(OPTIONS_COUNT - 1)
        if (distractors.isEmpty()) return null

        val options = (distractors + answer).shuffled(random)
        return WordRushQuestion(
            word = word,
            direction = direction,
            options = options,
            correctIndex = options.indexOf(answer),
        )
    }

    private fun Word.answerFor(direction: WordRushDirection): String = when (direction) {
        WordRushDirection.Recognize -> translation
        WordRushDirection.Recall -> originalWord
    }

    private fun WordRushDirection.flipped() = when (this) {
        WordRushDirection.Recognize -> WordRushDirection.Recall
        WordRushDirection.Recall -> WordRushDirection.Recognize
    }

    private fun String.normalized() = trim().lowercase()
}
