package domain.ai

import domain.ai.model.AiGuidance
import domain.ai.model.AiInstruction
import domain.ai.model.VocabularyFocus
import domain.ai.model.WordType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiInstructionTest {

    @Test
    fun `of returns null for blank text`() {
        assertNull(AiInstruction.of(""))
        assertNull(AiInstruction.of("  \n\t "))
    }

    @Test
    fun `of trims and single-spaces the text`() {
        assertEquals("only the verbs", AiInstruction.of("  only the\n\n verbs ")?.text)
    }

    @Test
    fun `of cuts text to the maximum length`() {
        assertEquals(AiInstruction.MAX_LENGTH, AiInstruction.of("a".repeat(AiInstruction.MAX_LENGTH + 50))?.text?.length)
    }

    @Test
    fun `guidance is guided only with the learner's own words`() {
        assertFalse(AiGuidance(VocabularyFocus(wordTypes = setOf(WordType.VERB))).isGuided)
        assertTrue(AiGuidance(instruction = AiInstruction.of("verbs")).isGuided)
    }

    @Test
    fun `focus is empty only when nothing is picked`() {
        assertTrue(VocabularyFocus().isEmpty)
        assertFalse(VocabularyFocus(skipNames = true).isEmpty)
    }
}
