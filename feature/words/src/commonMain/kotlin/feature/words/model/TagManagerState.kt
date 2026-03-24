package feature.words.model

import domain.tag.model.Tag

data class TagManagerState(
    val tags: List<Tag> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class TagManagerEffect {
    data class TagCreated(val tag: Tag) : TagManagerEffect()
    data class TagRenamed(val tag: Tag) : TagManagerEffect()
    data class TagDeleted(val tagId: Long) : TagManagerEffect()
    data class Error(val message: String) : TagManagerEffect()
}
