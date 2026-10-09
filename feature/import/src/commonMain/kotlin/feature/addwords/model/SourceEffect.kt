package feature.addwords.model

import domain.word.add.model.WordDraft
import domain.word.add.parser.RejectedLine

/** What a candidate-producing source (file, photo, AI) tells the sheet. */
sealed interface SourceEffect {
    data class CandidatesReady(
        val drafts: List<WordDraft>,
        val rejected: List<RejectedLine> = emptyList(),
    ) : SourceEffect

    /** The premium-only source was refused by the server: access ended since the sheet opened. */
    data object PremiumLapsed : SourceEffect
}
