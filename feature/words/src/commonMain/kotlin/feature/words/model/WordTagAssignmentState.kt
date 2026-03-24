package feature.words.model

import domain.tag.model.Tag

data class WordTagAssignmentState(
    val wordId: Int = 0,
    val tags: List<Tag> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false
)

sealed class WordTagAssignmentEffect {
    data object TagsAssigned : WordTagAssignmentEffect()
    data class Error(val message: String) : WordTagAssignmentEffect()
}
