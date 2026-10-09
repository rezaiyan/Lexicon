package feature.addwords.model

import core.common.Try
import core.common.map
import domain.word.add.model.WordDraft
import domain.word.add.model.WordOrigin
import domain.word.add.parser.RejectedLine

/** One reviewable word. [id] is stable for the life of the review (list keys, edits). */
data class Candidate(val id: Int, val draft: WordDraft, val selected: Boolean = true)

/**
 * Words found by a source (file, photo, AI) waiting for the user to check them before they are added.
 * Pure value: every operation returns a new review.
 */
data class CandidateReview(
    val origin: WordOrigin,
    val candidates: List<Candidate>,
    val rejected: List<RejectedLine> = emptyList(),
    val editingId: Int? = null,
) {
    val selectedDrafts: List<WordDraft> get() = candidates.filter { it.selected }.map { it.draft }
    val selectedCount: Int get() = candidates.count { it.selected }
    val allSelected: Boolean get() = candidates.isNotEmpty() && candidates.all { it.selected }
    val editing: Candidate? get() = candidates.firstOrNull { it.id == editingId }

    fun toggle(id: Int) = copy(
        candidates = candidates.map { if (it.id == id) it.copy(selected = !it.selected) else it },
    )

    fun setAllSelected(selected: Boolean) = copy(candidates = candidates.map { it.copy(selected = selected) })

    fun remove(id: Int) = copy(
        candidates = candidates.filterNot { it.id == id },
        editingId = editingId.takeUnless { it == id },
    )

    fun startEditing(id: Int) = if (candidates.any { it.id == id }) copy(editingId = id) else this

    fun cancelEditing() = copy(editingId = null)

    /** Replaces the word being edited; fails (review unchanged) when the edit is not a valid word. */
    fun saveEdit(term: String, translation: String, note: String): Try<CandidateReview> {
        val id = editingId ?: return Try.success(this)
        return WordDraft.of(term, translation, note).map { draft ->
            copy(
                candidates = candidates.map { if (it.id == id) it.copy(draft = draft, selected = true) else it },
                editingId = null,
            )
        }
    }

    companion object {
        fun of(origin: WordOrigin, drafts: List<WordDraft>, rejected: List<RejectedLine> = emptyList()) =
            CandidateReview(origin, drafts.mapIndexed { index, draft -> Candidate(index, draft) }, rejected)
    }
}
