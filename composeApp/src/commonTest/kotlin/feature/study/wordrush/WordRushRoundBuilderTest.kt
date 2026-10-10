package feature.study.wordrush

import domain.word.model.Word
import domain.wordrush.WordRushRoundBuilder
import domain.wordrush.model.WordRushDirection
import utils.Language
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WordRushRoundBuilderTest {

    private fun word(
        id: Int,
        term: String = "term_$id",
        translation: String = "meaning_$id",
        target: Language = Language.GERMAN,
    ) = Word(
        id = id,
        originalWord = term,
        translation = translation,
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = target,
        nextReviewDate = 0L,
    )

    @Test
    fun `build when pool has shared translations never repeats an option`() {
        val pool = listOf(
            word(1, translation = "house"),
            word(2, translation = "House "),
            word(3, translation = "house"),
            word(4, translation = "tree"),
            word(5, translation = "car"),
            word(6, translation = "dog"),
        )

        repeat(50) { seed ->
            WordRushRoundBuilder.build(pool, pool, Random(seed)).forEach { question ->
                val normalized = question.options.map { it.trim().lowercase() }
                assertEquals(normalized.size, normalized.toSet().size, "Duplicate options: ${question.options}")
            }
        }
    }

    @Test
    fun `build points correctIndex at the asked side of the card`() {
        val pool = (1..8).map { word(it) }

        repeat(20) { seed ->
            WordRushRoundBuilder.build(pool, pool, Random(seed)).forEach { question ->
                val expected = when (question.direction) {
                    WordRushDirection.Recognize -> question.word.translation
                    WordRushDirection.Recall -> question.word.originalWord
                }
                assertEquals(expected, question.answer)
                assertEquals(WordRushRoundBuilder.OPTIONS_COUNT, question.options.size)
            }
        }
    }

    @Test
    fun `build draws distractors from the whole pool not only the round`() {
        val round = listOf(word(1))
        val pool = round + (2..5).map { word(it) }

        val question = WordRushRoundBuilder.build(round, pool, Random(1)).single()

        assertEquals(WordRushRoundBuilder.OPTIONS_COUNT, question.options.size)
    }

    @Test
    fun `build prefers distractors from the same language pair`() {
        val german = (1..4).map { word(it, target = Language.GERMAN) }
        val spanish = (10..20).map { word(it, target = Language.SPANISH) }
        val pool = german + spanish

        repeat(20) { seed ->
            val question = WordRushRoundBuilder.build(german.take(1), pool, Random(seed)).single()
            val germanAnswers = german.flatMap { listOf(it.translation, it.originalWord) }.toSet()
            assertTrue(question.options.all { it in germanAnswers }, "Mixed languages: ${question.options}")
        }
    }

    @Test
    fun `build mixes recall questions into a round`() {
        val pool = (1..40).map { word(it) }

        val directions = WordRushRoundBuilder.build(pool, pool, Random(7)).map { it.direction }.toSet()

        assertEquals(setOf(WordRushDirection.Recognize, WordRushDirection.Recall), directions)
    }

    @Test
    fun `build skips a word when no distinct distractor exists on either side`() {
        val pool = listOf(
            word(1, term = "same", translation = "same"),
            word(2, term = "same", translation = "same"),
        )

        assertTrue(WordRushRoundBuilder.build(pool, pool, Random(0)).isEmpty())
    }
}
