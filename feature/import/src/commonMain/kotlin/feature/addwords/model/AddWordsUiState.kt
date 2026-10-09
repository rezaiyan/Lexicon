package feature.addwords.model

import domain.tag.model.Tag
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordOrigin
import utils.Language

/** Shared context of one add-words sheet: languages, tag, the review in progress and the final result. */
data class AddWordsUiState(
    val learning: Language? = null,
    val native: Language? = null,
    val languagesLoaded: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val selectedTagId: Long? = null,
    val hasPremiumTools: Boolean = false,
    val review: CandidateReview? = null,
    val isCommitting: Boolean = false,
    val problem: AddWordsProblem? = null,
    val result: AddWordsResult? = null,
) {
    val languages: LanguagePair?
        get() = if (learning != null && native != null) LanguagePair.orNull(learning, native) else null

    val tagIds: Set<Long> get() = setOfNotNull(selectedTagId)
}

data class AddWordsResult(
    val origin: WordOrigin,
    val added: Int,
    val duplicates: Int,
    val previewTerms: List<String>,
)

sealed interface AddWordsEffect {
    data object OpenReview : AddWordsEffect
    data object ShowResult : AddWordsEffect
}
