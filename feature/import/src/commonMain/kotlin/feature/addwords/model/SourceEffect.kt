package feature.addwords.model

import domain.word.add.model.WordDraft
import domain.word.add.parser.RejectedLine

/** What a candidate-producing source (file, photo, AI) tells the sheet. */
sealed interface SourceEffect {
    data class CandidatesReady(
        val drafts: List<WordDraft>,
        val rejected: List<RejectedLine> = emptyList(),
    ) : SourceEffect

    /** The server refused a paid source: the balance is below its cost (it changed since the sheet opened). */
    data object OutOfCredits : SourceEffect
}
