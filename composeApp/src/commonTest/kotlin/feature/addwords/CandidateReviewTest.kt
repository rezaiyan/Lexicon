package feature.addwords

import core.common.getOrThrow
import domain.word.add.model.WordDraft
import domain.word.add.model.WordOrigin
import feature.addwords.model.CandidateReview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CandidateReviewTest {

    private fun draft(term: String, translation: String = "t") = WordDraft.of(term, translation).getOrThrow()

    private val review = CandidateReview.of(WordOrigin.Photo, listOf(draft("Hund"), draft("Katze"), draft("Maus")))

    @Test
    fun `of selects every candidate`() {
        assertEquals(3, review.selectedCount)
        assertTrue(review.allSelected)
    }

    @Test
    fun `AI and photo results lock the languages - they were translated for that pair`() {
        assertTrue(CandidateReview.of(WordOrigin.Photo, listOf(draft("Hund"))).languagesLocked)
        assertTrue(CandidateReview.of(WordOrigin.AiSuggestion, listOf(draft("Hund"))).languagesLocked)
    }

    @Test
    fun `file results keep the languages changeable - nothing was translated`() {
        assertFalse(CandidateReview.of(WordOrigin.File, listOf(draft("Hund"))).languagesLocked)
    }

    @Test
    fun `toggle removes a word from the selection only`() {
        val toggled = review.toggle(1)

        assertEquals(listOf("Hund", "Maus"), toggled.selectedDrafts.map { it.term })
        assertEquals(3, toggled.candidates.size)
    }

    @Test
    fun `setAllSelected false clears the selection`() {
        assertEquals(0, review.setAllSelected(false).selectedCount)
    }

    @Test
    fun `remove drops the word and stops editing it`() {
        val removed = review.startEditing(1).remove(1)

        assertEquals(listOf("Hund", "Maus"), removed.candidates.map { it.draft.term })
        assertNull(removed.editingId)
    }

    @Test
    fun `saveEdit replaces the edited word and reselects it`() {
        val edited = review.toggle(0).startEditing(0).saveEdit("der Hund", "the dog", "pet").getOrThrow()

        val first = edited.candidates.first()
        assertEquals("der Hund", first.draft.term)
        assertEquals("pet", first.draft.note)
        assertTrue(first.selected)
        assertNull(edited.editingId)
    }

    @Test
    fun `saveEdit with blank translation fails and keeps editing`() {
        val editing = review.startEditing(0)

        assertFalse(editing.saveEdit("Hund", " ", "").isSuccess)
        assertEquals(0, editing.editingId)
    }

    @Test
    fun `startEditing ignores unknown ids`() {
        assertNull(review.startEditing(42).editingId)
    }

    @Test
    fun `saveEdit keeps the tags the file gave the word`() {
        val tagged = CandidateReview.of(WordOrigin.File, listOf(WordDraft.of("Hund", "dog", tags = listOf("A1")).getOrThrow()))

        val edited = tagged.startEditing(0).saveEdit("der Hund", "the dog", "").getOrThrow()

        assertEquals(listOf("A1"), edited.candidates.single().draft.tags)
    }
}
