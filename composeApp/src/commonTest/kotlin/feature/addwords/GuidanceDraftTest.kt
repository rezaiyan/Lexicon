package feature.addwords

import core.error.DomainError
import core.error.DomainError.AddWords.InstructionsRefused.Reason
import domain.ai.model.AiInstruction
import domain.ai.model.Register
import domain.ai.model.WordType
import feature.addwords.model.GuidanceDraft
import feature.addwords.model.InstructionProblem
import feature.addwords.model.toInstructionProblem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GuidanceDraftTest {

    @Test
    fun `withText caps the length and clears a shown problem`() {
        val draft = GuidanceDraft(problem = InstructionProblem.OffTopic)
            .withText("x".repeat(AiInstruction.MAX_LENGTH + 10))

        assertEquals(AiInstruction.MAX_LENGTH, draft.text.length)
        assertNull(draft.problem)
    }

    @Test
    fun `a starter fills an empty field ready to be finished`() {
        assertEquals("I'm preparing for ", GuidanceDraft().withStarter("I'm preparing for").text)
    }

    @Test
    fun `a starter continues what is already written`() {
        assertEquals("verbs. For my trip to ", GuidanceDraft(text = "verbs.  ").withStarter("For my trip to").text)
    }

    @Test
    fun `only text makes the draft guided - quick picks stay free`() {
        val picks = GuidanceDraft().toggleWordType(WordType.VERB).toggleGrammarNotes()

        assertFalse(picks.isGuided)
        assertTrue(picks.withText("hospital words").isGuided)
        assertFalse(picks.withText("   ").isGuided)
    }

    @Test
    fun `word types toggle on and off`() {
        val draft = GuidanceDraft().toggleWordType(WordType.NOUN).toggleWordType(WordType.VERB)

        assertEquals(setOf(WordType.NOUN, WordType.VERB), draft.focus.wordTypes)
        assertEquals(setOf(WordType.VERB), draft.toggleWordType(WordType.NOUN).focus.wordTypes)
    }

    @Test
    fun `one register at most - and picking it again clears it`() {
        val formal = GuidanceDraft().toggleRegister(Register.FORMAL)

        assertEquals(Register.SLANG, formal.toggleRegister(Register.SLANG).focus.register)
        assertNull(formal.toggleRegister(Register.FORMAL).focus.register)
    }

    @Test
    fun `photo picks toggle`() {
        val draft = GuidanceDraft().toggleMarkedOnly().toggleSkipNames()

        assertTrue(draft.focus.markedOnly)
        assertTrue(draft.focus.skipNames)
    }

    @Test
    fun `a pause survives quick-pick edits and blocks writing - other problems clear`() {
        val paused = GuidanceDraft(problem = InstructionProblem.Paused).toggleGrammarNotes()
        val offTopic = GuidanceDraft(problem = InstructionProblem.OffTopic).toggleGrammarNotes()

        assertEquals(InstructionProblem.Paused, paused.problem)
        assertFalse(paused.canWrite)
        assertNull(offTopic.problem)
        assertTrue(offTopic.canWrite)
    }

    @Test
    fun `toGuidance carries picks and the normalized instruction`() {
        val guidance = GuidanceDraft(text = " only  verbs ").toggleMarkedOnly().toGuidance()

        assertEquals("only verbs", guidance.instruction?.text)
        assertTrue(guidance.focus.markedOnly)
    }

    @Test
    fun `refusals map to instruction problems - other errors don't`() {
        assertEquals(
            InstructionProblem.PersonalData,
            DomainError.AddWords.InstructionsRefused(Reason.PersonalData).toInstructionProblem(),
        )
        assertEquals(InstructionProblem.Paused, DomainError.AddWords.InstructionsRefused(Reason.Paused).toInstructionProblem())
        assertNull(DomainError.Commerce.InsufficientCredits.toInstructionProblem())
    }
}
