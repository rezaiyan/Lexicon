package domain.listening

import domain.listening.model.ListeningCommand
import domain.listening.model.ListeningOrder
import domain.listening.model.ListeningReducer
import domain.listening.model.ListeningSession
import domain.listening.model.ListeningSettings
import domain.listening.model.ListeningStep
import domain.listening.model.ListeningTransition
import domain.listening.model.Utterance
import domain.word.model.Word
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ListeningReducerTest {

    private fun word(id: Int) = Word(
        id = id,
        originalWord = "Hund$id",
        translation = "dog$id",
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.GERMAN,
        nextReviewDate = 0L,
    )

    private val twoWords = ListeningSession(listOf(word(1), word(2)))

    private fun ListeningTransition.session() = (this as ListeningTransition.Continue).session

    @Test
    fun `StepCompleted walks prompt pause answer gap then next word`() {
        var session = twoWords
        val steps = mutableListOf(session.step)
        repeat(4) {
            session = ListeningReducer.reduce(session, ListeningCommand.StepCompleted, repeatCount = 1).session()
            steps += session.step
        }

        assertEquals(
            listOf(
                ListeningStep.Prompt,
                ListeningStep.Pause,
                ListeningStep.Answer,
                ListeningStep.Gap,
                ListeningStep.Prompt,
            ),
            steps,
        )
        assertEquals(1, session.index)
    }

    @Test
    fun `StepCompleted plays the example between answer and gap when the word has one`() {
        val withExample = ListeningSession(listOf(word(1).copy(description = "Der Hund bellt.")))
        val answer = withExample.copy(step = ListeningStep.Answer)

        val example = ListeningReducer.reduce(answer, ListeningCommand.StepCompleted, repeatCount = 1).session()
        assertEquals(ListeningStep.Example, example.step)

        val gap = ListeningReducer.reduce(example, ListeningCommand.StepCompleted, repeatCount = 1).session()
        assertEquals(ListeningStep.Gap, gap.step)
    }

    @Test
    fun `StepCompleted skips the example when the word's example is blank`() {
        val blank = ListeningSession(listOf(word(1).copy(description = "   ")))

        val next = ListeningReducer.reduce(
            blank.copy(step = ListeningStep.Answer),
            ListeningCommand.StepCompleted,
            repeatCount = 1,
        ).session()

        assertEquals(ListeningStep.Gap, next.step)
    }

    @Test
    fun `example is spoken trimmed in the learned word's language`() {
        val withExample = ListeningSession(listOf(word(1).copy(description = "  Der Hund bellt. ")))

        assertEquals(Utterance("Der Hund bellt.", "de"), withExample.example())
        assertEquals(null, twoWords.example())
    }

    @Test
    fun `StepCompleted on last gap finishes the session`() {
        val lastGap = twoWords.copy(index = 1, step = ListeningStep.Gap)

        val result = ListeningReducer.reduce(lastGap, ListeningCommand.StepCompleted, repeatCount = 1)

        assertIs<ListeningTransition.Finished>(result)
    }

    @Test
    fun `StepCompleted on gap repeats same word until repeatCount reached`() {
        val gap = twoWords.copy(step = ListeningStep.Gap)

        val repeated = ListeningReducer.reduce(gap, ListeningCommand.StepCompleted, repeatCount = 2).session()
        assertEquals(0, repeated.index)
        assertEquals(1, repeated.repetition)
        assertEquals(ListeningStep.Prompt, repeated.step)

        val advanced = ListeningReducer.reduce(
            repeated.copy(step = ListeningStep.Gap),
            ListeningCommand.StepCompleted,
            repeatCount = 2,
        ).session()
        assertEquals(1, advanced.index)
        assertEquals(0, advanced.repetition)
    }

    @Test
    fun `Next mid word jumps to next prompt and resets repetition`() {
        val midWord = twoWords.copy(step = ListeningStep.Answer, repetition = 1)

        val next = ListeningReducer.reduce(midWord, ListeningCommand.Next, repeatCount = 2).session()

        assertEquals(1, next.index)
        assertEquals(ListeningStep.Prompt, next.step)
        assertEquals(0, next.repetition)
    }

    @Test
    fun `Next on last word finishes`() {
        val last = twoWords.copy(index = 1)

        assertIs<ListeningTransition.Finished>(ListeningReducer.reduce(last, ListeningCommand.Next, repeatCount = 1))
    }

    @Test
    fun `Previous on first word restarts it from the prompt`() {
        val firstAnswer = twoWords.copy(step = ListeningStep.Answer)

        val previous = ListeningReducer.reduce(firstAnswer, ListeningCommand.Previous, repeatCount = 1).session()

        assertEquals(0, previous.index)
        assertEquals(ListeningStep.Prompt, previous.step)
    }

    @Test
    fun `Previous goes back one word`() {
        val second = twoWords.copy(index = 1, step = ListeningStep.Pause)

        assertEquals(0, ListeningReducer.reduce(second, ListeningCommand.Previous, repeatCount = 1).session().index)
    }

    @Test
    fun `prompt and answer follow read order and each side's language`() {
        assertEquals(Utterance("Hund1", "de"), twoWords.prompt(ListeningOrder.WORD_FIRST))
        assertEquals(Utterance("dog1", "en"), twoWords.answer(ListeningOrder.WORD_FIRST))
        assertEquals(Utterance("dog1", "en"), twoWords.prompt(ListeningOrder.TRANSLATION_FIRST))
        assertEquals(Utterance("Hund1", "de"), twoWords.answer(ListeningOrder.TRANSLATION_FIRST))
    }

    @Test
    fun `answer is revealed only from the answer step on`() {
        assertFalse(twoWords.isAnswerRevealed)
        assertFalse(twoWords.copy(step = ListeningStep.Pause).isAnswerRevealed)
        assertTrue(twoWords.copy(step = ListeningStep.Answer).isAnswerRevealed)
        assertTrue(twoWords.copy(step = ListeningStep.Example).isAnswerRevealed)
        assertTrue(twoWords.copy(step = ListeningStep.Gap).isAnswerRevealed)
    }

    @Test
    fun `gap is half the pause but never below the minimum`() {
        assertEquals(1_500L, ListeningSettings(pauseMs = 3_000L).gapMs)
        assertEquals(ListeningSettings.MIN_GAP_MS, ListeningSettings(pauseMs = 1_000L).gapMs)
    }
}
